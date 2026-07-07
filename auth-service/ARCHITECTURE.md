# auth-service — Architecture

The platform's **identity provider and the single JWT issuer**. It is the only service that
holds the RSA private key and the only place that decides *who a user is* and *how they may log
in*. Every other service merely verifies the tokens this service mints (see
[`core-security/ARCHITECTURE.md`](../core-security/ARCHITECTURE.md)).

Runnable Spring Boot application (`bootJar`). It depends on:

- **`core-security`** — token machinery (`JwtService`, `AuthCookieManager`, `AuthenticatedUser`,
  `Audience`/`TokenType`, `PasswordEncoder`, the security filter chain). auth-service never signs
  a JWT by hand; it always goes through `JwtService`.
- **`user-service`** — the `User` domain model, `UserRole`/`EntityStatus`, and `UserRepository`
  (a **MongoDB** repository). This is the user datastore auth-service reads and, when
  provisioning, writes.

---

## 1. Responsibilities

| Concern | Owner |
|---|---|
| Staff password login (internal) | `InternalAuthService` |
| Customer / partner OAuth login (OIDC ID token) | `OAuthService` |
| Verify a provider ID token (signature/iss/exp/aud) | `OAuthTokenVerifier` |
| Map a verified identity → platform `User` (+ JIT provisioning) | `OAuthUserResolver` |
| Adapt a `User` → `TokenRequest` and mint access+refresh | `AuthTokenIssuer` |
| Refresh / rotate / logout of the staff session | `InternalAuthService` |
| Sign the JWT, set the cookies | ❌ `core-security` (`JwtService`, `AuthCookieManager`) |
| Verify our own JWT on later requests | ❌ `core-security` filter (in every service) |
| What a role may *do* | ❌ each resource service (`@PreAuthorize`) |

**One axis of variation: `Audience`.** `STAFF` authenticates by password; `CUSTOMER` and
`PARTNER` authenticate by OIDC ID token. Everything downstream of authentication (mint access +
refresh, set cookies) is identical — that shared tail lives in `AuthTokenIssuer`.

---

## 2. Component map

```
controller/                         thin HTTP adapters; fix the Audience, delegate to a service
  InternalAuthController   /auth-api/v1/auth/internal/{login,refreshToken,rotateToken,logout}
  CustomerAuthController   /auth-api/v1/auth/customer/login        (Audience = CUSTOMER)
  PartnerAuthController    /auth-api/v1/auth/partner/login         (Audience = PARTNER)

dto/
  AuthRequest              union request: {audience, authProvider, idToken, username, password, roles}

service/
  InternalAuthService(Impl)   staff password login + refresh/rotate/logout
  OAuthService(Impl)          verify ID token → resolve user → check active → issue
  support/AuthTokenIssuer     User → TokenRequest → JwtService.issue(ACCESS/REFRESH) → cookies

oauth/                          the OAuth/OIDC verification + user-mapping subsystem
  OAuthTokenVerifier          Nimbus decoder per provider; verifies sig/iss/exp/aud, returns identity
  OAuthIdentity               immutable identity asserted by the verified ID token
  OAuthUserResolver           SPI: identity → User (pluggable)
  impl/DefaultOAuthUserResolver   match by verified email, enforce provider link, optional JIT provision

config/
  OAuthProperties          app.oauth.* provider registry + provisioning flag

exception/
  OAuthAuthenticationException   → HTTP 401

AuthenticationServiceApplication   @SpringBootApplication(scanBasePackages = "com.restaurantos")
```

The wide component-scan (`com.restaurantos`) is what pulls in `core-security` (filter, resolvers,
`JwtService`, …) and `user-service` (`UserRepository`).

---

## 3. Authentication flows

### 3.1 STAFF — internal password login
```
POST /auth-api/v1/auth/internal/login   { audience: STAFF, username, password }
  InternalAuthServiceImpl.authenticate
    user = userRepository.findByUsername(username)           // Mongo
    require request.isStaff() AND passwordEncoder.matches(password, user.password)   // BCrypt
    AuthTokenIssuer.issue(user, STAFF, response)
      → access + refresh JWT, set as HttpOnly cookies (TTL: 15m / 1d)
  200, empty body; credentials live only in the cookies
```

Session lifecycle for staff:

| Endpoint | Effect |
|---|---|
| `POST /internal/refreshToken` | Verify the **refresh** cookie, re-issue **access only**, keep the same refresh token (sliding access, fixed refresh window). |
| `POST /internal/rotateToken` | Verify the refresh cookie, issue a **new access _and_ refresh** pair (full rotation). |
| `POST /internal/logout` | Clear both cookies. This is client-side only; to invalidate a live access token before it expires, wire a `TokenRevocationChecker` bean in core-security (e.g. a `jti` denylist) — the SPI is already consulted on every verify. |

`refreshToken` / `rotateToken` both go through `loadUserFromRefreshToken`, which rejects anything
whose `tokenType != REFRESH`, then reloads the user (so role/tenant changes take effect on refresh).

### 3.2 CUSTOMER / PARTNER — OAuth (OIDC) login
```
POST /auth-api/v1/auth/{customer|partner}/login   { authProvider: "google", idToken: "<OIDC JWT>" }
  OAuthServiceImpl.authenticate(request, audience, response)
    1. identity = OAuthTokenVerifier.verify(authProvider, idToken)     // trust the token, not the body
    2. user     = OAuthUserResolver.resolve(identity, audience)        // match by verified email / JIT
    3. require user.isEnabled()                                        // ACTIVE accounts only
    4. AuthTokenIssuer.issue(user, audience, response)                 // our own access + refresh
```

**Identity comes from the cryptographically-verified ID token, never from the request body.** The
client sends only `authProvider` + `idToken`; email, subject, name, and `email_verified` are read
off the verified token (`OAuthIdentity`).

`OAuthTokenVerifier` builds one `NimbusJwtDecoder` per provider (lazily, cached in a
`ConcurrentHashMap`) from `app.oauth.providers.<name>`. It validates signature (against the
provider's JWKS — discovered from `issuer` or an explicit `jwksUri`), `iss`, `exp`, and — when
`audiences` is configured — that the token's `aud` intersects our registered client IDs. If
`audiences` is empty it logs a warning and skips the `aud` check.

`DefaultOAuthUserResolver` requires a **verified** email, matches an existing `User` by that
email, and enforces that the account is linked to the *same* provider. When no account exists it
provisions one **only if** `app.oauth.provisioning.enabled` — customers as `ACTIVE`/`CUSTOMER`,
partners as `PENDING_APPROVAL`/`DELIVERY_PARTNER` (so a partner cannot obtain tokens until vetted,
because step 3 requires `isEnabled()`).

---

## 4. Configuration

Two profiles ship in the module:

- **`application.yml`** — production shape, fully env-driven. The MongoDB URI (`MONGODB_URI`), JWT
  keys, cookie policy, CORS, and OAuth providers all come from environment variables; actuator
  health probes on.
- **`application-localdev.yml`** — local defaults: port `8181`, `cookie.secure=false` +
  `same-site=Lax` (plain-HTTP dev), keys loaded from the classpath, and the shared MongoDB
  `user_db` at `localhost:27019`.

Key property groups:

| Prefix | Bound type | Purpose |
|---|---|---|
| `app.security.jwt.*` | `core-security` `SecurityProperties` | issuer, **private-key** (issuer only), public-key, clock-skew, per-audience TTLs, cookie policy, CORS |
| `app.oauth.providers.<name>` | `OAuthProperties.Provider` | OIDC `issuer`, optional `jwksUri`, accepted `audiences` (our client IDs) |
| `app.oauth.provisioning.enabled` | `OAuthProperties.Provisioning` | JIT-create a user on first OAuth login (default `false`) |

Adding a provider (e.g. Apple) is **configuration only** — a new block under
`app.oauth.providers` with its issuer and client IDs; no code change.

---

## 5. Invariants — what must not change

Contracts other services, clients, or the security model depend on. Treat these as fixed unless
you are doing a coordinated, platform-wide change.

1. **auth-service is the only issuer.** It is the only holder of `app.security.jwt.private-key`.
   Never configure a private key on any other service, and never move token *signing* out of this
   service (always go through `core-security` `JwtService`).
2. **Public API prefix.** All login/refresh/logout routes live under `/auth-api/v1/auth/**`, which
   is the `permitAll` matcher in `core-security` `SecurityConfig`. Moving a route out of that
   prefix makes it require a token (chicken-and-egg for login); changing the prefix must be done in
   both places at once.
3. **Trust the proof, not the request.** For OAuth, identity fields (email, subject, name) come
   from the verified ID token — never from `AuthRequest`. For staff, the password is checked with
   the shared BCrypt `PasswordEncoder`. Roles and `restaurantCodes` on the minted token always come
   from the resolved `User`, **not** from `AuthRequest.roles` (which is not honoured for privilege).
4. **Verifier settings.** `OAuthTokenVerifier` must always validate signature + issuer + expiry.
   In any real environment, set `audiences` per provider so `aud` is enforced.
5. **Cookies via `AuthCookieManager`.** Auth cookies are always written/cleared through
   core-security's `AuthCookieManager` (HttpOnly + configured Secure/SameSite). Do not hand-roll
   `Set-Cookie`.
6. **Provisioning safety defaults.** JIT provisioning stays **off by default**; partners provision
   as `PENDING_APPROVAL` and must pass the `isEnabled()` gate. Don't auto-activate partner accounts.
7. **`AuthTokenIssuer` is the single mint-and-set-cookies path.** Every successful login funnels
   through it, so the access/refresh pairing and cookie policy stay consistent across all audiences.

---

## 6. Persistence

The only datastore auth-service touches is **MongoDB**, through `user-service`'s `UserRepository`
(the `users` collection). auth-service reads users on every login and, when JIT provisioning is
enabled, writes new ones. It owns no schema of its own and runs no relational/JPA datasource.

- **Local:** `spring.data.mongodb.uri` → the shared `user_db` (`docker-compose` → `user-db`,
  `localhost:27019`).
- **Production:** `MONGODB_URI` env var.

`DefaultOAuthUserResolver` is the single place that writes to this shared store — the seam to
replace with a `user-service` HTTP call once the services are fully split (see §7).

---

## 7. How to extend

- **New OAuth provider** — add `app.oauth.providers.<name>` (`issuer`, `audiences`); no code.
- **New audience** — add the value to `core-security` `Audience` (and a TTL), then add a thin
  controller that fixes it. If the login is OAuth-based, reuse `OAuthService` as-is; if it is
  credential-based, add a service modelled on `InternalAuthService`. `AuthTokenIssuer` is unchanged.
- **New login method for an existing audience** (e.g. customer magic-link) — add an endpoint +
  service that authenticates the user its own way, then calls `AuthTokenIssuer.issue(user, audience,
  response)`. The token model and every verifier are unaffected.
- **Move off the shared user datastore** — implement `OAuthUserResolver` (and the equivalent for
  the staff flow) to call `user-service` over HTTP instead of `UserRepository`.
  `DefaultOAuthUserResolver` is the single seam that writes to the shared store today.
- **Custom matching / provisioning policy** — provide your own `OAuthUserResolver` bean; it
  replaces the default by type.

---

## 8. Endpoint reference

| Method | Path | Audience | Body | Result |
|---|---|---|---|---|
| POST | `/auth-api/v1/auth/internal/login` | STAFF | `{username, password, audience:STAFF}` | access+refresh cookies |
| POST | `/auth-api/v1/auth/internal/refreshToken` | STAFF | *(refresh cookie)* | new access cookie |
| POST | `/auth-api/v1/auth/internal/rotateToken` | STAFF | *(refresh cookie)* | new access+refresh cookies |
| POST | `/auth-api/v1/auth/internal/logout` | STAFF | — | cookies cleared |
| POST | `/auth-api/v1/auth/customer/login` | CUSTOMER | `{authProvider, idToken}` | access+refresh cookies |
| POST | `/auth-api/v1/auth/partner/login` | PARTNER | `{authProvider, idToken}` | access+refresh cookies |

All success responses are `200` with an empty body; the credential material is delivered entirely
via `Set-Cookie`. Failures surface as JSON `401`/`403` via `core-security`'s
`RestSecurityErrorHandler`, or `401` from `OAuthAuthenticationException`.
