# core-security — Architecture

Shared, framework-level security library imported by every runnable service. It owns
**stateless JWT authentication** for the whole platform and is designed to keep working
unchanged after each service is split into its own independently-deployable microservice.

It is a plain library (`jar`, not `bootJar`) — no `main`, starts nothing on its own. It
contributes beans, a Spring Security filter chain, MVC argument resolvers, and configuration
properties into whatever Spring Boot application component-scans `com.restaurantos`.

> **Consumers.** `identity-service` is the single **issuer** (signs tokens). Every other service
> (`menu`, `order`, …) is a **verifier**. A consumer adds
> `implementation project(':core-security')` and component-scans `com.restaurantos` — services
> already do via `@SpringBootApplication(scanBasePackages = "com.restaurantos")`. A verifier that
> uses `@PreAuthorize` also needs `spring-boot-starter-security` on its own compile classpath
> (core-security exposes it only at runtime).

---

## 1. Design in one picture

```
                RS256 asymmetric signing (issuer signs, everyone verifies)

  identity-service (ISSUER)                     resource service (VERIFIER)
  ├─ has RSA PRIVATE key  ── signs ──►  JWT  ──► ├─ has RSA PUBLIC key only
  │  (app_private.pem, secret)                    │  (bundled in core-security jar)
  └─ JwtService.issue(...)                        └─ JwtService.verify(...) → AuthenticatedUser
```

**Why asymmetric.** With a shared HMAC secret, *every* service could forge tokens and the
secret had to be copied into every service's config. With RS256 only `auth-service` can mint
tokens; verifiers hold the **public** key (not a secret), which ships inside the core-security
jar — so a verifier needs **zero** JWT configuration and there is nothing sensitive to
duplicate. Only `identity-service` can mint tokens. This is the standard microservices JWT model
and survives the service split.

---

## 2. Responsibilities

| Concern | Owner |
|---|---|
| Sign a JWT (RS256, private key) | `JwtService.issue` — **issuer only** |
| Verify signature + issuer + expiry, return identity | `JwtService.verify` / `tryVerify` |
| Turn a request into a Spring `Authentication` | `JwtAuthenticationFilter` |
| Reject refresh tokens used as access tokens | `JwtAuthenticationFilter` (checks `tokenType`) |
| Expose `@CurrentUser` / `@RestaurantCodes` | resolvers + `WebMvcConfig` |
| Stateless chain, CORS, JSON 401/403, `PasswordEncoder` | `SecurityConfig` |
| Read/write auth cookies | `AuthCookieManager` |
| Token revocation hook | `TokenRevocationChecker` (SPI; no-op default) |
| Reusable tenant-scope / view checks | `ScopeGuard` (see §8) |
| **Who** a user is / credentials / login methods | ❌ `identity-service` |
| **What** a role may do (`@PreAuthorize`) | ❌ each service (see §8) |

The boundary is deliberate: this module knows how to *prove* an identity and carry it, but
nothing about *user records*, *passwords*, *login methods*, or *per-endpoint authorization*. It
depends on no service's domain model — callers adapt their model into `TokenRequest`.

---

## 3. Invariants — what must not change without a platform-wide migration

These are load-bearing contracts. Changing any of them is a breaking change that must be rolled
out to **every** service (issuer and all verifiers) at once, because tokens in flight and on the
wire span services and versions.

1. **Signing algorithm & key model — RS256, one private key.** The issuer signs with the RSA
   private key; verifiers hold only the public key. Do not reintroduce a shared symmetric secret,
   and do not give a verifier a private key. Changing the algorithm re-issues every live token.
2. **`issuer` claim.** Every token carries `iss` and every verifier requires it
   (`requireIssuer`). The value (`restaurant-os` by default) must be identical across all
   services. Changing it invalidates all outstanding tokens.
3. **Claim wire format.** `roles` and `restaurantCodes` travel as a **comma-separated string**
   (`JwtTokenUtil.join/splitClaim`), `tokenType` as the enum name, `aud` as the `Audience` name.
   `JwtServiceImpl.issue` and `JwtServiceImpl.verify` are two halves of one contract — change one,
   change the other, and redeploy issuer + verifiers together.
4. **`tokenType` semantics.** Only `ACCESS` tokens authenticate a request; the filter rejects
   `REFRESH` tokens as bearer credentials. Do not relax this — it is what stops a long-lived
   refresh token from being replayed as an access token.
5. **`AuthenticatedUser` is the principal.** It is what the filter stores and what
   `@CurrentUser` / `@RestaurantCodes` read. It implements `Principal`; keep it immutable and keep
   `getName()` returning the username. Downstream code binds to this type.
6. **Public matchers.** `/auth-api/v1/auth/**` and `/actuator/health/**` are `permitAll`; every
   other route is `authenticated`. The issuer's login endpoints live under that prefix by
   contract — moving either side breaks login for the whole platform.
7. **Role authority normalisation.** Roles are stored *without* the `ROLE_` prefix and
   `SecurityAuthorities` adds it exactly once. `hasRole('X')` across all services depends on this.
8. **The token is parsed exactly once**, in `JwtAuthenticationFilter`. Resolvers and controllers
   read the already-materialised principal; they must not re-parse the token.

Everything outside this list (TTLs, cookie policy, CORS, revocation strategy, adding claims or
audiences) is designed to be extended — see §9.

---

## 4. Package layout

```
annotation/   CurrentUser, RestaurantCodes                 controller parameter markers
authz/        ScopeGuard                                    reusable tenant-scope + view + role checks
config/
  SecurityConfig            stateless chain + CORS + entrypoints + revocation default bean
  SecurityProperties        app.security.jwt.* (keys, issuer, skew, cookie, cors, ttl)
  JwtAuthenticationFilter   verify once → set AuthenticatedUser principal (ACCESS only)
  WebMvcConfig              registers the argument resolvers
enums/        Audience (STAFF|PARTNER|CUSTOMER), TokenType (ACCESS|REFRESH), CookieName
exception/    InvalidTokenException                         thrown by verify()
model/
  TokenRequest              domain-neutral input to issue()  (username, roles, codes, audience)
  AuthenticatedUser         verified identity; the Spring principal (implements Principal)
resolver/
  AbstractPrincipalArgumentResolver   reads AuthenticatedUser from SecurityContext (shared base)
  CurrentUserArgumentResolver         @CurrentUser → username (String) or AuthenticatedUser
  RestaurantCodesArgumentResolver     @RestaurantCodes → Set<String>
revocation/   TokenRevocationChecker                        SPI; default bean returns false
service/      JwtService / impl.JwtServiceImpl              issue + verify (RS256)
util/
  KeyLoader                 PEM (inline | classpath: | file) → RSA keys
  JwtTokenUtil              extractToken, readCookie, join/split CSV claims
  SecurityAuthorities       role names → ROLE_-prefixed authorities (exactly once)
  AuthCookieManager         set/clear/read auth cookies per configured cookie policy
web/          RestSecurityErrorHandler                      JSON 401 + 403
resources/keys/app_public.pem   public verification key, on every consumer's classpath
```

---

## 5. Request flow

**Issue (auth-service only):**
```
TokenRequest(username, roles, restaurantCodes, audience)
  → JwtService.issue(request, ACCESS|REFRESH)
      claims: iss, aud, sub, jti, iat, exp, tokenType, roles(csv), restaurantCodes(csv)
      signed RS256 with the private key; TTL from SecurityProperties.ttlFor(audience)
  → AuthCookieManager.setAuthCookies(...)   (HttpOnly, Secure?, SameSite per config)
```

**Verify (every request, any service):**
```
JwtAuthenticationFilter (extends OncePerRequestFilter)
  token = JwtTokenUtil.extractToken(request)        // Bearer header, else accessToken cookie
  short-circuit if token blank OR context already authenticated
  jwtService.tryVerify(token)                        // signature + issuer + expiry(+skew)
     → reject unless tokenType == ACCESS             // refresh tokens can't authenticate
     → reject if revocationChecker.isRevoked(jti)
     → principal = AuthenticatedUser, authorities = ROLE_-prefixed roles
  SecurityConfig: /auth-api/v1/auth/** and /actuator/health/** = public; else authenticated
  Controllers: @CurrentUser / @RestaurantCodes read straight from the principal (no re-parse)
```

The filter never throws on a bad token: `tryVerify` returns `Optional.empty()` and the request
proceeds unauthenticated, to be rejected (401) by the entry point if the route requires auth.

---

## 6. Configuration

`app.security.jwt.*` — every value has a production-safe default, so a **verifier needs no
`app.security.jwt` block at all**.

| Property | Default | Who sets it |
|---|---|---|
| `issuer` | `restaurant-os` | must match across all services |
| `public-key` | `classpath:keys/app_public.pem` | default resolves to the jar-bundled key |
| `private-key` | *(none)* | **issuer only** (auth-service) |
| `clock-skew` | `PT30S` | optional |
| `ttl.<audience>.access-token-ttl` / `.refresh-token-ttl` | STAFF 15m/1d, PARTNER 30m/7d, CUSTOMER 30m/30d | issuer only |
| `cookie.secure` / `cookie.same-site` / `cookie.path` / `cookie.domain` | `true` / `Strict` / `/` / none | issuer only (only it sets cookies) |
| `cors.enabled` / `cors.allowed-origins` / `cors.allowed-methods` / `cors.allowed-headers` / `cors.allow-credentials` | `false` / `[]` / (CRUD+OPTIONS) / `*` / `true` | edge/browser-facing services |

A key/PEM location (`public-key` / `private-key`) may be an **inline PEM string**, a
`classpath:` resource, or a **filesystem path** (`KeyLoader.readPem`). This lets production inject
the private key via env var (`APP_SECURITY_JWT_PRIVATE_KEY=<inline PEM>`) or a mounted secret
without code changes.

---

## 7. Key management

- **Local dev.** Neither key is committed (`.gitignore` → `**/keys/*.pem`). Generate the pair
  once after cloning with `scripts/generate-dev-keys.sh` — it writes the private key into
  `auth-service` and the matching public key into `core-security`. Re-run to rotate.
- **Production.** Do **not** ship the private key in the image. Set
  `APP_SECURITY_JWT_PRIVATE_KEY` (inline PEM) or point `private-key` at a mounted secret.
  Rotate by generating a new keypair, distributing the public key, then switching the issuer.
- **Rotation / multi-service future.** Replace the bundled public key with a **JWKS endpoint**
  on auth-service (`/.well-known/jwks.json`) and a caching verifier, so keys rotate without
  redeploying every service. `KeyLoader` is the seam to extend.

---

## 8. Authorization (enforced by each service; primitives live here)

This module authenticates and carries identity; it does **not** decide what a role may do. Each
service authorizes its own endpoints, in two layers:

1. **Role gating — declarative.** `@EnableMethodSecurity` is on and roles arrive as `ROLE_*`
   authorities, so `@PreAuthorize("hasAnyRole('OWNER','MANAGER')")` works end-to-end. Never write
   the `ROLE_` prefix (invariant §3.7 adds it). A verifier using `@PreAuthorize` must have
   `spring-boot-starter-security` on its compile classpath.

2. **Tenant scope — data-aware, via `ScopeGuard`.** Role annotations can't express "does this record
   belong to a restaurant the caller controls", because that depends on the payload/record.
   `authz/ScopeGuard` reads the caller's `AuthenticatedUser` from the `SecurityContext` and offers:

   | Method | Rule |
   |---|---|
   | `assertWithinScope(codes)` | **writes** — caller's `restaurantCodes` must be a **superset** of `codes` (empty denied) |
   | `assertCanView(codes)` | **reads** — caller's scope must **intersect** `codes` |
   | `isPlatformAdmin()` / `hasRole(r)` / `hasRestaurantScope()` / `callerUsername()` | role & scope predicates |

   `ADMIN` (`ScopeGuard.PLATFORM_ADMIN_ROLE`) bypasses both scope checks — it is the one
   platform-global role. Denials throw `AccessDeniedException` → HTTP 403 (see §9 error handling).

Services compose `ScopeGuard` for their own model: `identity-service`'s `AccessGuard` adds
`UserRole`-typed role helpers and a privilege-escalation guard; `order-service`'s `OrderAccessGuard`
adds order **ownership** (a customer owns the orders they place) on top of scope. Menu-service uses
`ScopeGuard` directly.

Always derive the caller's tenant from the verified token (`@RestaurantCodes` or `ScopeGuard`),
**never** from a request-body field — the token can't be spoofed by the client.

### Error handling

`GlobalSecurityExceptionHandler` (a `@RestControllerAdvice` at `HIGHEST_PRECEDENCE`) maps
`AccessDeniedException` → 403 and `AuthenticationException` → 401 as `ApiError` JSON, for **every**
service — before any service-local `@ExceptionHandler(Exception.class)` can swallow them as 500.
`RestSecurityErrorHandler` produces the same shape for failures caught in the filter chain
(missing/expired token). Domain 404s stay in each service's own advice.

---

## 9. How to extend

- **New audience (e.g. `KIOSK`)** — add to `Audience`; the `ttlFor` switch forces you to set its
  lifetime, and optionally override via `ttl.kiosk` on the issuer. Signing/verifying and the
  filter need no changes (single keypair).
- **New login method for an existing audience** (e.g. customer magic-link) — add a login
  endpoint/service in auth-service that mints a token for the same `Audience`; the token model is
  unchanged. Method is orthogonal to audience — record it as an `amr` claim if you need to know it.
- **New claim** — add a name to `CookieName`, put it in `JwtServiceImpl.issue`, read it in
  `verify` into `AuthenticatedUser`. Multi-valued? use `JwtTokenUtil.join/splitClaim`. (This
  touches invariant §3.3 — roll out issuer and verifiers together.)
- **New principal-derived controller param** — subclass `AbstractPrincipalArgumentResolver`,
  add a marker annotation, register it in `WebMvcConfig`.
- **Real revocation** — declare a `TokenRevocationChecker` bean (e.g. Redis denylist by `jti`);
  it replaces the no-op default automatically (`@ConditionalOnMissingBean`).
- **Per-service security rules** — declare your own `SecurityFilterChain` bean to override the
  shared one; enable CORS via `app.security.jwt.cors.*`.

---

## 10. Production checklist

- [ ] Private key injected via env/secret manager; **not** committed. Public key distributed to verifiers.
- [ ] `cookie.secure=true` and an appropriate `same-site` behind HTTPS.
- [ ] `cors.enabled=true` with explicit `allowed-origins` on browser-facing services.
- [ ] Consider RS256 → JWKS if you rotate keys or add many services.
- [ ] Provide a `TokenRevocationChecker` if logout/compromise must invalidate live tokens.
- [ ] Short access TTL + refresh rotation (`rotateToken`) for sensitive flows.

---

## 11. Known limitations / roadmap

- **Revocation** is a no-op by default (§9) — logout clears cookies but the access token stays
  valid until expiry unless a checker is wired.
- **No audience enforcement on verify.** The `aud` claim is available on `AuthenticatedUser` but
  the shared chain doesn't restrict it; a service that must only accept e.g. `STAFF` tokens should
  check `audience` in `@PreAuthorize` or its own filter.
- **Public key is bundled**, not fetched — fine for now; JWKS is the scale-out path.
- **`restaurantCodes` travel in the token** — a user in very many restaurants can bloat the
  cookie past ~4 KB; move to a scope lookup if that becomes real.

---

## 12. Changelog

### authorization primitives
- **`ScopeGuard`** (`authz/`) — reusable tenant-scope (`assertWithinScope`), read-visibility
  (`assertCanView`), and role/scope predicates, `ADMIN`-bypassed. Reused by identity, menu, and
  order; identity's `AccessGuard` now delegates its scope logic to it.
- **`GlobalSecurityExceptionHandler` + `ApiError`** (`web/`) — one `@RestControllerAdvice`
  (HIGHEST_PRECEDENCE) renders 401/403 as `{code,message}` JSON platform-wide, so services stop
  re-declaring it and `@PreAuthorize`/`ScopeGuard` denials never get masked as 500.

### 2026-07-02 — audience model
- **`AuthType {INTERNAL, CUSTOMER, OTP}` → `Audience {STAFF, PARTNER, CUSTOMER}`**, carried in the
  standard `aud` claim. Audience ("who the token is for") is now separated from method ("how they
  logged in"): STAFF = internal password, PARTNER + CUSTOMER = OAuth.
- **Removed OTP** entirely (controller/service/impl, `OtpToken`, `OtpTokenRepository`).
- **Consolidated OAuth**: one `OAuthService` serves both customer and partner via thin
  audience-fixing controllers (`/auth/customer`, `/auth/partner`) — no duplicated flow.
- Per-audience TTLs: STAFF 15m/1d, PARTNER 30m/7d, CUSTOMER 30m/30d.

### 2026-07-02 — production-grade rework
- **HS256 shared secret → RS256 asymmetric.** Issuer signs with a private key; verifiers use the
  bundled public key. Removed all HMAC secrets and the multi-key brute-force parser.
- **Removed secret duplication.** The four copied `app.security.jwt` secret blocks are gone;
  verifier services now need no JWT config.
- **Fixed dead role authorization.** Roles are written and read as CSV consistently and
  normalised to `ROLE_*`, so `hasRole(...)` works (previously roles never left the token and
  everyone silently got `ROLE_USER`).
- **`tokenType` claim + enforcement.** Access vs refresh tokens are now distinguishable; the
  filter refuses to authenticate with a refresh token.
- **Single verify path.** Filter verifies once and stores an `AuthenticatedUser` principal;
  resolvers read from it (removed duplicated token-parsing across resolvers).
- **`CookieUtil` (auth-service) → `AuthCookieManager` (core-security)** with a configurable
  cookie policy (`secure`/`same-site`/`domain`), so local HTTP dev and HTTPS prod both work.
- **Added:** issuer + clock-skew validation, JSON 401/403 (`RestSecurityErrorHandler`),
  configurable CORS, pluggable `TokenRevocationChecker`, and `jti` for future revocation.
- Constructor injection throughout; `InvalidTokenException` decouples callers from the JWT lib.

### earlier — hardening pass
- jjwt 0.11.5 → 0.12.6 and off the deprecated API; `boolean refreshToken` → `TokenType`
  (fixing an inverted access/refresh TTL bug); `/auth/**` → `/auth-api/v1/auth/**` public
  matcher (fixing a login lockout); `RestaurantCodesArgumentResolver` NPE guard.
