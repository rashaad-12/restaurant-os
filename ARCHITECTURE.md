# Restaurant-OS — Architecture Review

> A unified restaurant-management platform built as a Gradle multi-module Spring Boot project.
> This document rates the current architecture, records design decisions, and lists drawbacks,
> design smells, and concrete cleanup/improvement suggestions.

**Review date:** 2026-07-02
**Scope reviewed:** `common`, `core-security`, `user-service`, `auth-service`, `menu-service`, `order-service`, `elastic-service`, build & compose config.

---

## 1. Overall Rating

| Dimension | Score | Notes |
|---|---|---|
| Module structure & layering | 7 / 10 | Clean layered separation, interface + impl, shared libs. |
| Microservice boundaries / autonomy | 3 / 10 | `auth-service` reads `user-service`'s database directly — no real service isolation. |
| Security design | 4 / 10 | Good token/cookie hygiene, but **no authorization enforcement**, committed secrets, stubbed auth. |
| API contract design | 4 / 10 | Free-text `String` responses, no validation, no pagination, bulk-only endpoints. |
| Code quality & consistency | 5 / 10 | Readable, but heavy duplication, field injection, copy-paste bugs. |
| Operational readiness | 3 / 10 | No gateway, discovery, config server, tracing, resilience. |
| Testing | 3 / 10 | Only `menu-service` has meaningful tests (Testcontainers). |

**Overall: ~5 / 10 — a promising, cleanly-layered early-stage skeleton with strong security *scaffolding*, but not yet a true microservices architecture, and carrying several correctness and security defects that must be fixed before it can ship.**

---

## 2. Current Design

### 2.1 Topology

```
                         ┌──────────────────────────────────────────────┐
                         │   (No API gateway / discovery / config yet)   │
                         └──────────────────────────────────────────────┘

   identity-service               menu-service       order-service
   (Mongo: user_db)               (Mongo: menu_db)   (MySQL: order_db)
   auth + user/restaurant domain
   ← former auth-service + user-service, merged (one owner of user_db)

   elastic-service (partial)   ──►  Elasticsearch 9.2 + Kibana

   Shared libraries:  core-security  (JWT, filters, resolvers, SecurityConfig)
                      common         (DateUtil, JsonUtil, DateConstants)

   Declared in settings.gradle but NOT present on disk:
     app, kitchen-service, payment-service, analytic-service,
     inventory-service, notification-service
```

### 2.2 Technology choices

- **Runtime:** Java 17, Spring Boot 3.5.5, Gradle multi-module monorepo.
- **Polyglot persistence:** MongoDB for document-shaped data (`menu`, `user`), MySQL for relational data (`auth`, `order`). Elasticsearch + Kibana for search/analytics.
- **AuthN:** Stateless JWT (HS256) delivered via `HttpOnly` + `Secure` + `SameSite=Strict` cookies. Three auth types (`INTERNAL`, `CUSTOMER`, `OTP`), each with its own signing key and access/refresh TTLs, configured under `app.security.jwt.*`.
- **Mapping:** MapStruct (`toEntity` / `toDTO` / `updateEntityFromDTO`).
- **Cross-cutting security ergonomics:** custom `HandlerMethodArgumentResolver`s expose `@CurrentUser` (username) and `@RestaurantCodes` (tenant scope) to controllers, decoded from the JWT.

### 2.3 Per-service layering

`Controller → Service (interface) → ServiceImpl → Repository → Model`, with `DTO`s at the edge, `enums` for status, MapStruct `mapper`s between layers, and a `@RestControllerAdvice` exception handler per service.

### 2.4 Domain modelling highlights

- **Multi-tenancy** by `restaurantCode`: queries are scoped with `...In(restaurantCodes)` and the scope is derived from the token, not the request body.
- **Order state machine:** `OrderServiceImpl.ALLOWED_TRANSITIONS` explicitly encodes legal status transitions.
- **Auditing:** `@CreatedDate` / `@LastModifiedDate` on entities (JPA + Mongo).

---

## 3. Strengths

1. **Consistent layered architecture** across every service — easy to navigate and onboard.
2. **Shared `core-security` library** centralises JWT parsing, the auth filter, and the security config, keeping auth logic DRY across services.
3. **Good cookie hygiene** — `HttpOnly`, `Secure`, `SameSite=Strict`, explicit `maxAge`.
4. **Per-auth-type signing keys & TTLs** — clean separation between internal staff, customers, and OTP flows; tokens self-describe their `AuthType` so the right key is selected.
5. **Custom argument resolvers** (`@CurrentUser`, `@RestaurantCodes`) keep controllers thin and make tenant scoping declarative.
6. **MapStruct** eliminates hand-written mapping boilerplate.
7. **Explicit order state machine** rather than ad-hoc status mutation.
8. **Polyglot persistence** matched to workload (document menus vs. relational orders).
9. **Testcontainers** integration testing in `menu-service` (realistic Mongo, not mocks).

---

## 4. Drawbacks & Design Smells

Severity: 🔴 critical · 🟠 major · 🟡 minor

| # | Sev | Finding | Location |
|---|---|---|---|
| 1 | ✅ | **~~Shared database across services.~~** *Resolved:* `auth-service` and `user-service` are merged into **`identity-service`**, a single owner of `user_db`. The cross-service `project(':user-service')` dependency is gone; the `UserRepository`/`User` access is now in-process within one module. | `identity-service/` |
| 2 | ✅ | **~~No authorization enforcement.~~** *Resolved:* `@PreAuthorize` role gating + tenant-scope enforcement are in place across `identity-service` (users/restaurants), `menu-service`, and `order-service`. Reusable scope logic lives in `core-security` `ScopeGuard`; order-service adds ownership (customer self-service). | `core-security/authz/ScopeGuard`, `*/security/*Guard`, controllers |
| 3 | 🔴 | **Committed secrets.** JWT signing secrets live in `application-localdev.yml` (and are duplicated in every service's yml). DB credentials (`dev/dev`, `root`) are in `docker-compose.yml`. | `*/application-localdev.yml`, `docker-compose.yml` |
| 4 | 🔴 | **Access/refresh token TTLs are inverted.** All three auth impls call `generateToken(..., true)` (refresh flag → **refresh** TTL) and assign the result to the **access** token, and `false` (access TTL) to the **refresh** token. Access tokens therefore live as long as refresh tokens and vice-versa. | `CustomerAuthServiceImpl`, `InternalAuthServiceImpl`, `OtpAuthServiceImpl` |
| 5 | 🔴 | **Stubbed / bypassable auth.** OTP `sendOtp` is empty and `verifyOtp` hard-codes `isValidOtp = true`; customer login only string-compares `oauthProvider` (marked `TODO: implement proper OAuth provider verification`) with no real token validation. If shipped, these are auth bypasses. | `OtpAuthServiceImpl`, `CustomerAuthServiceImpl` |
| 6 | 🟠 | **No inter-service communication strategy.** No Feign/RestClient/WebClient and no messaging (Kafka/RabbitMQ). An order→kitchen→payment→notification flow needs events; the only integration today is the shared-DB hack (#1). | project-wide |
| 7 | 🟠 | **No gateway / discovery / central config.** No Spring Cloud Gateway, Eureka/Consul, or config server. Clients must hard-code each service's port; cross-cutting edge concerns (routing, rate limiting, CORS) are unowned. | project-wide |
| 8 | 🟠 | **Phantom Gradle modules.** `settings.gradle` includes `app`, `kitchen-service`, `payment-service`, `analytic-service`, `inventory-service`, `notification-service` — none exist on disk. Aggregate build tasks are misleading and fragile. | `settings.gradle` |
| 9 | 🟠 | **`elastic-service` is broken scaffolding.** `@ComponentScan("com.restaurantos.restaurantservice")` targets a package that doesn't exist (won't scan its own beans); the client mixes a literal `"https"` host with a separate `scheme` variable. No repositories/controllers yet. | `ElasticSearchConfig.java` |
| 10 | 🟠 | **jjwt version drift + deprecated API.** `core-security` pins jjwt **0.11.5** and uses its deprecated builder (`parserBuilder`, `setSigningKey`, `setSubject`, `SignatureAlgorithm.HS256`); `auth-service` pins **0.12.5**. Divergent versions on one classpath risk runtime breakage. | `core-security/build.gradle`, `auth-service/build.gradle`, `JwtServiceImpl` |
| 11 | 🟠 | **Anemic API contracts.** Mutations return free-text `String` ("... was processed successfully") with no IDs and no per-item outcome; all write endpoints take bulk `List<DTO>` with silent partial success; **no `@Valid`/Bean Validation anywhere**; no pagination on `getAll`; verbs in paths (`/getAll`). | all controllers |
| 12 | 🟠 | **`@Transactional` on MongoDB services.** `menu`/`user` impls annotate multi-doc writes `@Transactional`, which requires a Mongo replica set; the single-node Mongo in `docker-compose` cannot honor them. | `MenuServiceImpl`, `UserServiceImpl` |
| 13 | 🟡 | **Massive duplication in `OrderServiceImpl`.** `accept/prepare/complete/reject/cancel` are near-identical; the same find→filter→mutate→saveAll shape repeats across menu/user/order. | `OrderServiceImpl`, others |
| 14 | 🟡 | **Copy-paste message bugs.** Order accept/prepare/complete/reject/cancel all return "... archive/publish request ...". `updateUser`/`archiveUser` return "No users to publish". `publishUser` actually approves. | `OrderServiceImpl`, `UserServiceImpl`/`Controller` |
| 15 | 🟡 | **Field injection everywhere** (`@Autowired` on fields) in controllers, services, filters, resolvers — hurts testability and immutability. | project-wide |
| 16 | 🟡 | **`RestaurantCodesArgumentResolver` NPE risk & duplicated logic.** `restaurantCodesClaim.toString()` with no null check throws when the claim is absent; it also re-implements splitting instead of reusing `JwtTokenUtil.splitClaim`. | `RestaurantCodesArgumentResolver` |
| 17 | 🟡 | **No token revocation / refresh store.** Roles & codes are baked into the JWT; a role change or logout can't invalidate a live token. An `OtpToken` entity + repository exist but are unused. | `core-security`, `auth-service` |
| 18 | 🟡 | **`common` pulls `spring-boot-starter-web`.** A shared util module drags the full web stack; `JsonUtil` also maintains its own `ObjectMapper` while `JwtServiceImpl` autowires another — inconsistent JSON config. | `common/build.gradle`, `JsonUtil` |
| 19 | 🟡 | **No-op `peek` side-effect in `createOrder`.** `.peek(order -> order.setOrderItems(order.getOrderItems()))` relies on an obscure setter side-effect to wire parent refs; intent is hidden. | `OrderServiceImpl.createOrder` |
| 20 | 🟡 | **No observability/resilience.** Only `menu-service` has actuator; no metrics, distributed tracing (Micrometer/OTel), health aggregation, timeouts (except elastic), retries, or circuit breakers. | project-wide |
| 21 | 🟡 | **`devtools` as a blanket subproject dependency**, including on libraries — should be `developmentOnly` on runnable apps only, and it already is, but it is applied to *every* subproject via the root build. | root `build.gradle` |

---

## 5. Code Cleanup Checklist

Concrete, low-risk changes that improve quality without changing architecture:

- [ ] Replace field `@Autowired` with constructor injection via Lombok `@RequiredArgsConstructor` (make dependencies `private final`).
- [ ] Collapse `OrderServiceImpl` accept/prepare/complete/reject/cancel into one `changeStatus(List<OrderDTO>, OrderStatus target)` guarded by `ALLOWED_TRANSITIONS`.
- [ ] Fix copy-paste return/log messages ("archive"/"publish" in order ops, "publish" in user ops); fix the "updation" wording.
- [ ] Add a null guard in `RestaurantCodesArgumentResolver` and reuse `JwtTokenUtil.splitClaim(...)`.
- [ ] Remove the no-op `peek` in `createOrder`; call `order.setOrderItems(...)` (or a named `wireItems()` helper) explicitly.
- [ ] Introduce a `TokenType {ACCESS, REFRESH}` enum instead of the boolean flag in `JwtService.generateToken` (kills the inversion bug class).
- [ ] Migrate `core-security` to jjwt **0.12.x** fluent API and align the version with `auth-service`.
- [ ] Trim `common` to jackson-only dependencies; expose a single shared `ObjectMapper` configuration and reuse it in `JwtServiceImpl`.
- [ ] Remove phantom modules from `settings.gradle` (or scaffold real ones) — keep the build graph honest.
- [ ] Fix `ElasticSearchConfig`'s `@ComponentScan` package and unify the scheme handling.
- [ ] Replace free-text `String` responses with a small `ApiResponse`/`BulkResult` DTO (counts + per-item status).

---

## 6. Recommended Improvements (by priority)

### P0 — Correctness & security (before any further feature work)
1. **Fix the access/refresh TTL inversion** (#4) and add a test asserting access TTL < refresh TTL.
2. ✅ **~~Add method-level authorization~~** (#2): *Done* — `@PreAuthorize` + `ScopeGuard` tenant-scope across identity, menu, and order; order adds customer ownership.
3. **Externalise secrets** (#3): move JWT keys and DB creds to environment variables / a secrets manager; keep only placeholders in committed yml; rotate the leaked keys.
4. **Finish or gate the stubbed auth flows** (#5): implement real OTP verification and OAuth token validation, or disable those endpoints until implemented.

### P1 — Service boundaries
5. ✅ **~~Break the shared DB~~** (#1): *Done* — `auth-service` + `user-service` merged into `identity-service`, one owner of `user_db`. If the domains need to diverge again later, re-split behind an HTTP contract (see `identity-service/ARCHITECTURE.md` §7).
6. **Introduce an API gateway** (Spring Cloud Gateway) for routing, CORS, rate limiting, and edge auth, plus **centralised config** (Spring Cloud Config / Kubernetes ConfigMaps).
7. **Adopt an integration pattern** (#6): event-driven messaging (Kafka/RabbitMQ) for the order → kitchen → payment → notification lifecycle; define event schemas.

### P2 — API & data integrity
8. **Add Bean Validation** (`@Valid` + `@NotBlank`/`@NotNull`) to all request DTOs.
9. **Return structured responses** with created IDs and per-item bulk results; add pagination to list endpoints; normalise REST paths.
10. **Resolve Mongo transaction semantics** (#12): run a replica set (even single-node with `--replSet`) or drop `@Transactional` where multi-doc atomicity isn't actually available.

### P3 — Operability & quality
11. Add **actuator + Micrometer + distributed tracing** across all services; standardise structured logging.
12. Add **resilience** (timeouts, retries, circuit breakers) on all outbound/inter-service calls.
13. Add a **token revocation / refresh-token store** (use the existing `OtpToken` scaffolding or a Redis blacklist).
14. **Raise test coverage** beyond `menu-service`; add contract tests for the security library and auth flows.
15. **Scaffold or remove** the phantom modules so the declared architecture matches reality.

---

## 7. Summary

Restaurant-OS is a **well-organised early-stage skeleton**: the layering is clean, the security library is a genuinely good idea, and the domain modelling (tenant scoping, order state machine, polyglot persistence) shows sound instincts. However, it is **not yet a microservices architecture** — services share a database and have no gateway, discovery, config, or messaging — and it carries **several shipping-blockers**: inverted token lifetimes, absent authorization, committed secrets, and stubbed authentication. Address the P0 security/correctness items first, then invest in real service boundaries (P1) before adding new domains. Do that, and this becomes a solid foundation.
