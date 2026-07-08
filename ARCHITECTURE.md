# Restaurant-OS — Architecture Review

> A unified restaurant-management platform built as a Gradle multi-module Spring Boot monorepo.
> This is the **platform-level** document: it rates the system as a whole, describes the current
> topology and cross-cutting patterns, and indexes each module's own architecture doc. For the
> internals of any one module, read its `ARCHITECTURE.md` (linked in §3).

**Review date:** 2026-07-08
**Scope reviewed:** `common`, `core-security`, `identity-service`, `menu-service`, `order-service`,
`elastic-service`, `analytic-service`, build & compose config.

---

## 1. Overall Rating

| Dimension | Score | Notes |
|---|---|---|
| Module structure & layering | 8 / 10 | Consistent `Controller → Service → Repository`, interface+impl, MapStruct, shared libs. Uniform and easy to navigate. |
| Security design (authN) | 8 / 10 | RS256 asymmetric JWT: one issuer signs, verifiers hold only the public key. Clean audience/token-type model, cookie hygiene, SYSTEM service tokens. |
| Authorization | 7 / 10 | Two-layer (role gate + `ScopeGuard` tenant scope) enforced in identity/menu/order, with order ownership. **Undermined by analytic-service having no auth at all.** |
| Microservice boundaries | 7 / 10 | Real boundaries now: shared DB removed (identity merge), CDC + enrichment for integration, one datastore per service. |
| Integration architecture | 8 / 10 | Debezium CDC → Kafka → enrichment → ES is a genuine, well-built event pipeline with DLQ and idempotent writes. |
| API contract design | 4 / 10 | Free-text `String` responses, bulk-only writes with silent partial success, no `@Valid`, no pagination on most lists. |
| Operational readiness | 4 / 10 | Still no gateway / discovery / central config; observability is health-only (no metrics/tracing); resilience thin outside the sync worker. |
| Testing | 4 / 10 | Strong in `core-security` and `menu-service`; the two ES services and order-service service-layer are largely untested. |

**Overall: ~6.5 / 10 — a genuinely maturing platform.** Since the last review the security model
went asymmetric (RS256), the shared-DB coupling was removed, and a real CDC/search pipeline landed.
It is now recognisably a microservices architecture. The remaining blockers are one **critical auth
hole** (analytic-service), **committed secrets**, and missing **edge/ops infrastructure** (gateway,
config, metrics/tracing) rather than structural flaws.

---

## 2. Current Topology

```
                         ┌──────────────────────────────────────────────┐
                         │   (No API gateway / discovery / config yet)   │
                         └──────────────────────────────────────────────┘

  identity-service ── ISSUER ──►  RS256 JWT (private key)  ──► every service verifies (public key)
  (Mongo user_db)                 STAFF / CUSTOMER / PARTNER / SYSTEM audiences

  menu-service          order-service           identity-service
  (Mongo menu_db)       (MySQL order_db)         (Mongo user_db)
        │                     │  binlog (ROW+GTID)
        │                     ▼
        │              Debezium (kafka-connect) ──► Kafka topic dev.restaurant.orders
        │                                                     │
        │                                                     ▼
        │                                        elastic-service (CDC sync worker)
        │                          enrichment ◄──── pulls docs from order-service (SYSTEM JWT)
        │                                                     │  bulk index
        │                                                     ▼
        │                                        Elasticsearch  dev_orders_<org>
        │                                                     ▲
        │                                                     │ read-only query/aggregation
        │                                        analytic-service  (⚠ no auth yet)

  Shared libraries:  core-security  (RS256 JWT, filter, resolvers, ScopeGuard, SecurityConfig)
                     common         (DateUtil, JsonUtil, DateConstants)

  Declared in settings.gradle but NOT present on disk (phantom):
     app, kitchen-service, payment-service, inventory-service, notification-service
```

---

## 3. Module Index

Each module owns a self-contained `ARCHITECTURE.md`. Ratings are from the per-module reviews
(2026-07-08).

| Module | Kind | Datastore | Rating | Doc |
|---|---|---|---|---|
| `core-security` | shared library (`jar`) | — | strong | [core-security/ARCHITECTURE.md](core-security/ARCHITECTURE.md) |
| `common` | shared library (`jar`) | — | thin/utility | [common/ARCHITECTURE.md](common/ARCHITECTURE.md) |
| `identity-service` | app — **JWT issuer**, user/restaurant domain | MongoDB `user_db` | strong | [identity-service/ARCHITECTURE.md](identity-service/ARCHITECTURE.md) |
| `menu-service` | app — menu domain | MongoDB `menu_db` | 6.5 / 10 | [menu-service/ARCHITECTURE.md](menu-service/ARCHITECTURE.md) |
| `order-service` | app — order domain (+ search enrichment) | MySQL `order_db` | 6 / 10 | [order-service/ARCHITECTURE.md](order-service/ARCHITECTURE.md) |
| `elastic-service` | app — CDC→ES sync worker (headless) | Elasticsearch (writes) | 8 / 10 | [elastic-service/ARCHITECTURE.md](elastic-service/ARCHITECTURE.md) |
| `analytic-service` | app — read-only ES query/analytics API | Elasticsearch (reads) | 6.5 / 10 ⚠ | [analytic-service/ARCHITECTURE.md](analytic-service/ARCHITECTURE.md) |

---

## 4. Technology & Cross-cutting Patterns

- **Runtime:** Java 17, Spring Boot 3.5.5, Gradle multi-module monorepo.
- **Polyglot persistence:** MongoDB (`user`, `menu` — document-shaped), MySQL (`order` — relational),
  Elasticsearch 9.2 + Kibana (search/analytics).
- **Eventing:** Kafka (KRaft) + Debezium CDC off the MySQL binlog (`docker-compose` enables
  `ROW`+GTID and runs `kafka-connect`), with a DLQ topic and Kafka-UI.
- **AuthN — asymmetric JWT.** `identity-service` is the **only issuer** (holds the RSA private key);
  every other service is a **verifier** holding only the public key bundled in `core-security`.
  Audiences: `STAFF` (password), `CUSTOMER`/`PARTNER` (OIDC), `SYSTEM` (service-to-service). Tokens
  ride in `HttpOnly`+`Secure`+`SameSite` cookies; `SYSTEM` tokens are fetched by client-id/secret.
- **AuthZ — two layers, in each service.** `@PreAuthorize` role gating **+** `core-security`
  `ScopeGuard` tenant-scope (superset for writes, intersection for reads; `ADMIN` global). Services
  extend it: identity's `AccessGuard` (privilege-escalation guard), order's `OrderAccessGuard`
  (per-record **ownership**). Tenant scope always comes from the token, never the request body.
- **Integration — CDC + enrichment (not shared DB).** The order → search flow is: DB change →
  Debezium → Kafka → `elastic-service` enriches by calling back into the owning service with a SYSTEM
  token → bulk-index into per-tenant ES indices. `analytic-service` queries those indices read-only.
- **Generic platform services.** `elastic-service` and `analytic-service` are deliberately
  domain-agnostic: sources, enrichment endpoints, index prefixes, and query shapes are configuration
  or runtime-introspected, so new domains are largely config, not code.
- **Mapping / edges:** MapStruct between layers; `@RestControllerAdvice` per service, with
  `core-security`'s `GlobalSecurityExceptionHandler` rendering 401/403 uniformly platform-wide.

---

## 5. Strengths

1. **Asymmetric security model done right** — single issuer, bundled public key, zero JWT config on
   verifiers, `tokenType`/`audience` enforcement, and a documented set of load-bearing invariants.
2. **Real service boundaries** — the old `auth`/`user` shared-DB coupling is gone (merged into
   `identity-service`); each service owns exactly one datastore.
3. **A production-shaped event pipeline** — CDC + enrichment + idempotent bulk writes + DLQ with
   backoff/jitter; at-least-once delivery with effectively-once outcomes.
4. **A schema-driven generic query engine** (`analytic-service`) with real DoS guardrails.
5. **Consistent, navigable layering** and shared libraries that keep auth logic DRY.
6. **Domain modelling instincts** — tenant scoping everywhere, an explicit order state machine,
   order ownership, polyglot persistence matched to workload.
7. **Every module is documented** — each has a self-contained, technically detailed `ARCHITECTURE.md`.
8. **Good tests where they matter most** — `core-security` (JWT/filter/scope) and `menu-service`
   (Testcontainers) are well covered.

---

## 6. Drawbacks & Design Smells

Severity: 🔴 critical · 🟠 major · 🟡 minor

| # | Sev | Finding | Location |
|---|---|---|---|
| 1 | 🔴 | **analytic-service has no authN/authZ.** No `core-security`, no `@PreAuthorize`; `orgCode` is an unauthenticated path param → any caller can read any tenant's order analytics. | `analytic-service` (see its doc §6) |
| 2 | 🔴 | **Committed secrets.** DB creds (`dev/dev`, `root`), ES creds (`admin/password`), and the elastic-service `client-secret` live in committed yml/compose. | `*/application-localdev.yml`, `docker-compose.yml` |
| 3 | 🟠 | **No gateway / discovery / central config.** Clients hard-code ports; routing, CORS, rate limiting, and edge auth are unowned; config is per-file, not centralised. | project-wide |
| 4 | 🟠 | **Anemic API contracts.** Free-text `String` responses, bulk-only writes with silent partial success, **no `@Valid`**, no pagination on most `getAll`, verb-in-path (`/getAll`). | menu/order/identity controllers |
| 5 | 🟠 | **Uneven testing.** ES services and the order-service service layer are largely untested; the highest-logic code (sync coalescing, the query builder) has no tests. | `elastic-service`, `analytic-service`, `order-service` |
| 6 | 🟠 | **Thin observability/resilience.** Health endpoints only — no metrics, no distributed tracing, no consumer-lag/DLQ metrics; retries/circuit-breakers only inside the sync worker. | project-wide |
| 7 | 🟡 | **Mongo `@Transactional` on single-node.** menu/identity annotate multi-doc writes `@Transactional`, non-atomic without a replica set. | `MenuServiceImpl`, identity |
| 8 | 🟡 | **No migrations where they'd help.** order-service uses `ddl-auto: update` (no Flyway); menu-service carries an unused Flyway dep it can't run against Mongo. | `order-service`, `menu-service` |
| 9 | 🟡 | **Duplication & copy-paste messages.** order-service's five status methods are near-identical with wrong "archive/publish" strings; menu-service repeats "No menus to publish". | `OrderServiceImpl`, `MenuServiceImpl` |
| 10 | 🟡 | **Phantom Gradle modules.** `settings.gradle` still includes `app`, `kitchen-service`, `payment-service`, `inventory-service`, `notification-service` — none exist on disk. | `settings.gradle` |
| 11 | 🟡 | **`common` drags the web stack.** A util `jar` pulls `spring-boot-starter-web`; duplicated `ObjectMapper` config across modules. | `common/build.gradle` |

**Resolved since 2026-07-02:** shared database across services (identity merge); no authorization
enforcement (two-layer authz in identity/menu/order); HS256 shared secret & TTL-inversion bug
(→ RS256, `TokenType`); no messaging / no inter-service strategy (CDC + Kafka + enrichment);
stubbed/bypassable auth (real OIDC verification, OTP removed); broken `elastic-service` scaffolding
(now a working pipeline).

---

## 7. Recommended Improvements (by priority)

### P0 — Security & correctness (before wider exposure)
1. **Authenticate analytic-service** (#1): add `core-security`, require a token, and derive the
   tenant from token scope (`ScopeGuard`) instead of the path `orgCode`.
2. **Externalise all secrets** (#2): env/secrets manager for DB, ES, and SYSTEM client credentials;
   keep only placeholders committed; enable TLS to ES/Kafka in prod.

### P1 — Edge & platform infrastructure
3. **API gateway** (Spring Cloud Gateway) for routing, CORS, rate limiting, and edge auth — front
   the generic query API especially.
4. **Centralised config** (Spring Cloud Config / Kubernetes ConfigMaps + Secrets) and **service
   discovery**, so ports/URLs stop being hard-coded.
5. **Observability baseline**: Micrometer → Prometheus across all services, OpenTelemetry tracing
   spanning the order → CDC → enrichment → index hop, consumer-lag/DLQ metrics on the sync worker.

### P2 — API & data integrity
6. **Structured responses**: replace free-text `String` with a shared `BulkResult`/`ApiResponse`
   (IDs + per-item outcome); add `@Valid` to all request DTOs; add pagination to list endpoints.
7. **Migrations & transactions**: Flyway + `ddl-auto: validate` on order-service; resolve Mongo
   `@Transactional` (single-node replica set or drop it); remove the unused Flyway dep from menu.
8. **Resilience** on inter-service calls (timeouts/retries/circuit-breakers) — generalise what the
   sync worker already does.

### P3 — Quality & housekeeping
9. **Raise test coverage** where it's thinnest and riskiest: the ES sync coalescing/late-delete
   logic, the analytic query builder, and the order-service service layer.
10. **De-duplicate** order-service status methods and fix copy-paste messages; trim `common` to
    Jackson-only and share one `ObjectMapper`.
11. **Honest build graph**: scaffold or remove the phantom modules in `settings.gradle`.

---

## 8. Summary

Restaurant-OS has moved from "cleanly-layered skeleton" to a **credible early microservices
platform**. The security model is now asymmetric and well-documented, services no longer share a
database, and a real CDC-driven search/analytics pipeline is in place — the two ES services are, in
design terms, the strongest code in the repo. The gating issues are now specific and mostly
non-structural: **analytic-service must be put behind authentication**, **secrets must leave the
repo**, and the platform needs its **edge and operational tier** (gateway, central config,
metrics/tracing) plus firmer **API contracts** and broader **tests**. Clear those, and this is a
solid foundation to add the remaining domains (kitchen, payment, inventory, notification) onto the
event backbone that already exists.
