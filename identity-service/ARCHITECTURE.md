# identity-service — Architecture

The platform's **identity provider and the single JWT issuer**, and the owner of the **user &
restaurant domain**. It is the only service that holds the RSA private key and the only place that
decides *who a user is* and *how they may log in*. Every other service merely verifies the tokens
this service mints (see [`core-security/ARCHITECTURE.md`](../core-security/ARCHITECTURE.md)).

> **Formed by merging `auth-service` + `user-service`.** auth-service already depended on
> `user-service` and read its `user_db` MongoDB directly through `UserRepository` — two deployables
> owning one datastore, i.e. coupling without isolation. Merging them removes that seam. Auth code
> lives under the `auth/` package; the user/restaurant domain (formerly user-service) under `user/`
> and `restaurant/`.

Runnable Spring Boot application (`bootJar`). It depends on:

- **`core-security`** — token machinery (`JwtService`, `AuthCookieManager`, `AuthenticatedUser`,
  `Audience`/`TokenType`, `PasswordEncoder`, the security filter chain). identity-service never
  signs a JWT by hand; it always goes through `JwtService`.

It owns **in-module** (no longer a cross-service dependency): the `User`/`Restaurant` domain models,
`UserRole`/`EntityStatus`, the MongoDB `UserRepository`/`RestaurantRepository`, and the
user/restaurant CRUD controllers (`/user-api/v1/user`, `/restaurant-api/v1/restaurant`).

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
| Authorize user/restaurant reads & writes (roles + tenant scope) | `@PreAuthorize` + `AccessGuard` |
| Sign the JWT, set the cookies | ❌ `core-security` (`JwtService`, `AuthCookieManager`) |
| Verify our own JWT on later requests | ❌ `core-security` filter (in every service) |
| What a role may *do* in **other** domains (menu, orders, …) | ❌ each resource service (`@PreAuthorize`) |

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

security/
  AccessGuard              data-aware authorization: tenant scope, read visibility, role checks,
                           privilege-escalation guard (see §4)

(auth code above lives under the `auth/` package; `config/OAuthProperties` + `config/MongoConfig`
are shared module config; `security/` is cross-domain authorization)

user/                           user domain — /user-api/v1/user CRUD  (controllers carry @PreAuthorize)
  controller · service · dto · mapper · model(User) · repository · enums · exception(local advice)
restaurant/                     restaurant domain — /restaurant-api/v1/restaurant CRUD  (@PreAuthorize)
  controller · service · dto · mapper · model(Restaurant) · repository · exception

IdentityServiceApplication   @SpringBootApplication(scanBasePackages = "com.restaurantos")
```

The wide component-scan (`com.restaurantos`) pulls in `core-security` (filter, resolvers,
`JwtService`, …) alongside this module's own auth, user, and restaurant beans.

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

## 4. Authorization

Authentication decides *who you are*; authorization decides *what you may see and do*. For the
user/restaurant APIs it is enforced here, in two layers:

1. **Role gating — declarative, at the controller.** `@PreAuthorize("hasAnyRole(...)")` on every
   write and on the management reads. Roles come from the JWT (the `core-security` filter maps them
   to `ROLE_*` authorities), so `hasRole('MANAGER')` matches a `MANAGER` claim — **never write the
   `ROLE_` prefix yourself**; `SecurityAuthorities` adds it.
2. **Tenant scope + no escalation — data-aware, in the service.** `security/AccessGuard` reads the
   caller's `AuthenticatedUser` from the `SecurityContext` and enforces what a role annotation
   cannot, because it depends on the request payload / the loaded record:
   - `assertWithinScope(codes)` — **writes**: the caller's `restaurantCodes` must be a **superset**
     of the codes in the request (an empty request is denied for non-admins).
   - `assertCanView(codes)` — **reads**: the caller's scope must **intersect** the resource's codes.
   - `assertNoPrivilegeEscalation(roles)` — a non-admin may not grant `ADMIN`/`OWNER`.
   - `requireAnyRole(...)` — role check where the decision is data-dependent (e.g. self vs. other).
   - **`ADMIN` bypasses all of the above** (platform-global).

### Write matrix (`@PreAuthorize`)

| Endpoint group | Roles | Also enforced in service |
|---|---|---|
| User create / update / approve / archive | ADMIN, OWNER, MANAGER | tenant scope + no escalation |
| User delete | ADMIN, OWNER | tenant scope |
| Restaurant create | ADMIN, OWNER | — (new code) |
| Restaurant update / archive | ADMIN, OWNER | tenant scope |
| Restaurant approve / delete | ADMIN | — |

### Read visibility

| Caller | Restaurants | Users |
|---|---|---|
| ADMIN | all | all |
| OWNER | own (scope) | users in own restaurants |
| MANAGER | ❌ 403 | users in own restaurants |
| SERVER / COOK / CUSTOMER / DELIVERY_PARTNER | ❌ 403 | **own record only** |

`getUserById` allows **self-access** — you can always read your own record; viewing anyone else
requires a managerial role *and* scope intersection. List/`getById` scoping is done at the DB via
`UserRepository.findByRestaurantCodesIn` / `RestaurantRepository.findByRestaurantCodeIn`.

### Error shape

Denials are `AccessDeniedException` → **403**; authentication failures → **401**; both render as
`{"code","message"}` JSON, consistently across *every* service via two `core-security` pieces:
`RestSecurityErrorHandler` (filter-chain failures — missing/expired token) and
`GlobalSecurityExceptionHandler` (a `HIGHEST_PRECEDENCE` `@RestControllerAdvice` that maps
`@PreAuthorize` and service-thrown denials before any local `@ExceptionHandler(Exception.class)` can
turn them into a 500). Domain 404s (`UserNotFoundException`, `RestaurantNotFoundException`) stay in
this module's own advice.

---

## 5. Configuration

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

## 6. Invariants — what must not change

Contracts other services, clients, or the security model depend on. Treat these as fixed unless
you are doing a coordinated, platform-wide change.

1. **identity-service is the only issuer.** It is the only holder of `app.security.jwt.private-key`.
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
8. **User/restaurant writes are role-gated *and* scope-enforced.** Every mutation carries a
   `@PreAuthorize` role check **and** passes through `AccessGuard` for tenant scope (and, for user
   writes, privilege-escalation). Reads are scoped too (§4). `ADMIN` is the only global role. Never
   add a write endpoint with only one of the two checks, and never honour `roles`/`restaurantCodes`
   from a request body for privilege — the token is the source of truth.

---

## 7. Persistence

The only datastore identity-service touches is **MongoDB**. The `User`/`Restaurant` documents and
their repositories (`UserRepository`, `RestaurantRepository`) live in this module (`user/`,
`restaurant/`). Auth reads users on every login and, when JIT provisioning is enabled, writes new
ones through the same repository — an in-process call now, not a cross-service one. It runs no
relational/JPA datasource.

- **Local:** `spring.data.mongodb.uri` → `user_db` (`docker-compose` → `user-db`, `localhost:27019`).
- **Production:** `MONGODB_URI` env var.

---

## 8. How to extend

- **New OAuth provider** — add `app.oauth.providers.<name>` (`issuer`, `audiences`); no code.
- **New audience** — add the value to `core-security` `Audience` (and a TTL), then add a thin
  controller that fixes it. If the login is OAuth-based, reuse `OAuthService` as-is; if it is
  credential-based, add a service modelled on `InternalAuthService`. `AuthTokenIssuer` is unchanged.
- **New login method for an existing audience** (e.g. customer magic-link) — add an endpoint +
  service that authenticates the user its own way, then calls `AuthTokenIssuer.issue(user, audience,
  response)`. The token model and every verifier are unaffected.
- **Split the user domain back out** — should identity and the user/restaurant domain need to
  become separate services again, `OAuthUserResolver` is the natural seam: reimplement it (and the
  staff-flow lookup) to call a user service over HTTP instead of the in-module `UserRepository`.
  `DefaultOAuthUserResolver` is the single place that writes to the store today.
- **Custom matching / provisioning policy** — provide your own `OAuthUserResolver` bean; it
  replaces the default by type.
- **New user/restaurant endpoint** — put a `@PreAuthorize` role check on the controller method, and
  if it touches tenant data call the matching `AccessGuard` method in the service (`assertWithinScope`
  for writes, `assertCanView` for reads, `assertNoPrivilegeEscalation` where roles are assignable).
  Denials become `403` automatically via `GlobalSecurityExceptionHandler`.
- **New role** — add it to `UserRole`; it becomes usable in `hasRole(...)` / `hasAnyRole(...)` and
  in `AccessGuard` immediately (roles flow from `User` → token → authorities unchanged).

---

## 9. Endpoint reference

| Method | Path | Audience | Body | Result |
|---|---|---|---|---|
| POST | `/auth-api/v1/auth/internal/login` | STAFF | `{username, password, audience:STAFF}` | access+refresh cookies |
| POST | `/auth-api/v1/auth/internal/refreshToken` | STAFF | *(refresh cookie)* | new access cookie |
| POST | `/auth-api/v1/auth/internal/rotateToken` | STAFF | *(refresh cookie)* | new access+refresh cookies |
| POST | `/auth-api/v1/auth/internal/logout` | STAFF | — | cookies cleared |
| POST | `/auth-api/v1/auth/customer/login` | CUSTOMER | `{authProvider, idToken}` | access+refresh cookies |
| POST | `/auth-api/v1/auth/partner/login` | PARTNER | `{authProvider, idToken}` | access+refresh cookies |

Login success responses are `200` with an empty body; the credential material is delivered entirely
via `Set-Cookie`. Failures surface as JSON `401`/`403` via `core-security`'s
`RestSecurityErrorHandler`, or `401` from `OAuthAuthenticationException`.

The `/auth-api/**` routes above are `permitAll`; everything below requires a valid access-token
cookie **and** the roles shown (see §4 for tenant scope and read visibility).

### User — `/user-api/v1/user`

| Method | Path | Roles | Notes |
|---|---|---|---|
| GET | `/{id}` | authenticated | self, or ADMIN/OWNER/MANAGER within scope |
| GET | `/getAll` | ADMIN, OWNER, MANAGER | ADMIN → all; OWNER/MANAGER → own restaurants |
| POST | `/` | ADMIN, OWNER, MANAGER | + scope + no escalation |
| PUT | `/` | ADMIN, OWNER, MANAGER | + scope + no escalation |
| PATCH | `/approve` | ADMIN, OWNER, MANAGER | + scope |
| PATCH | `/archive` | ADMIN, OWNER, MANAGER | + scope |
| DELETE | `/` | ADMIN, OWNER | + scope |

### Restaurant — `/restaurant-api/v1/restaurant`

| Method | Path | Roles | Notes |
|---|---|---|---|
| GET | `/{id}` | ADMIN, OWNER | ADMIN any; OWNER within scope |
| GET | `/getAll` | ADMIN, OWNER | ADMIN → all; OWNER → own |
| POST | `/` | ADMIN, OWNER | new code |
| PUT | `/` | ADMIN, OWNER | + scope |
| PATCH | `/approve` | ADMIN | platform vetting |
| PATCH | `/archive` | ADMIN, OWNER | + scope |
| DELETE | `/` | ADMIN | — |
