# Restaurant-OS — Platform Architecture

> A unified restaurant-management platform built as a Gradle multi-module Spring Boot monorepo.
> This is the **platform-level design document**: it describes the topology, the cross-cutting
> patterns every module inherits, and the rules a contributor must follow when extending the
> system. For the internals of any one module, read its `ARCHITECTURE.md` (indexed in §2).
>
> This file is intentionally the *only* architecture document published to the public repository.
> Live defects, security gaps, and the forward roadmap are tracked in `DEFECTS.md` and
> `ENHANCEMENTS.md`, which are **git-ignored** so we don't advertise weaknesses to the world.

---

## 1. Current Topology

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
        │                                        analytic-service

  Shared libraries:  core-security  (RS256 JWT, filter, resolvers, ScopeGuard, SecurityConfig)
                     common         (DateUtil, JsonUtil, DateConstants)
```

---

## 2. Module Index

Each module owns a self-contained `ARCHITECTURE.md` (pure design). Its live defects and roadmap,
where they exist, live in that module's git-ignored `DEFECTS.md` / `ENHANCEMENTS.md`.

| Module | Kind | Datastore | Doc |
|---|---|---|---|
| `core-security` | shared library (`jar`) | — | [core-security/ARCHITECTURE.md](core-security/ARCHITECTURE.md) |
| `common` | shared library (`jar`) | — | [common/ARCHITECTURE.md](common/ARCHITECTURE.md) |
| `identity-service` | app — **JWT issuer**, user/restaurant domain | MongoDB `user_db` | [identity-service/ARCHITECTURE.md](identity-service/ARCHITECTURE.md) |
| `menu-service` | app — menu domain | MongoDB `menu_db` | [menu-service/ARCHITECTURE.md](menu-service/ARCHITECTURE.md) |
| `order-service` | app — order domain (+ search enrichment) | MySQL `order_db` | [order-service/ARCHITECTURE.md](order-service/ARCHITECTURE.md) |
| `elastic-service` | app — CDC→ES sync worker (headless) | Elasticsearch (writes) | [elastic-service/ARCHITECTURE.md](elastic-service/ARCHITECTURE.md) |
| `analytic-service` | app — read-only ES query/analytics API | Elasticsearch (reads) | [analytic-service/ARCHITECTURE.md](analytic-service/ARCHITECTURE.md) |

---

## 3. Technology & Cross-cutting Patterns

- **Runtime:** Java 17, Spring Boot 3.5.5, Gradle multi-module monorepo.
- **Polyglot persistence:** MongoDB (`user`, `menu` — document-shaped), MySQL (`order` — relational),
  Elasticsearch 9.2 + Kibana (search/analytics). One datastore per service; no shared database.
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

## 4. Design Principles

1. **Asymmetric security, one issuer.** A single service signs; everyone else holds only the public
   key. Verifiers carry no JWT secret and no JWT configuration.
2. **Real service boundaries.** Each service owns exactly one datastore. Integrate through events and
   enrichment calls, never by reaching into another service's database.
3. **A production-shaped event pipeline.** CDC + enrichment + idempotent bulk writes + DLQ with
   backoff/jitter give at-least-once delivery with effectively-once outcomes.
4. **Generic where the domain doesn't belong.** The search/analytics tier is schema-driven and
   config-driven, so new domains onboard without new pipeline code.
5. **Consistent, navigable layering.** `Controller → Service → Repository`, interface+impl,
   MapStruct at boundaries, shared libraries that keep auth and JSON logic DRY.
6. **Tenant scoping everywhere.** The `restaurantCode` axis is the tenant key; scope is always
   derived from the verified token.
7. **Every module is documented.** Each has a self-contained `ARCHITECTURE.md`.

---

## 5. Extensibility Rules — Dos & Don'ts

These are platform-wide contracts. Anything that touches the JWT wire format or the public route
prefix is a **coordinated, deploy-everything-together** change.

**Do**
- **Do** derive the caller's identity, roles, and tenant scope from the verified token (`@CurrentUser`,
  `@RestaurantCodes`, `ScopeGuard`) — the token is the single source of truth.
- **Do** put both layers on every mutating endpoint: a `@PreAuthorize` role gate **and** a
  `ScopeGuard`/service-guard tenant-scope check.
- **Do** add a new domain to the search pipeline as **config** (`sync.sources.*` + an enrichment
  endpoint on the owning service), not as new code in `elastic-service`.
- **Do** integrate services through CDC events + enrichment, keeping the search-document shape owned
  by the domain service that produces it.
- **Do** externalise every credential (DB, ES, Kafka, SYSTEM client-secret, RSA private key) to
  env/secrets; commit only placeholders.
- **Do** route login/refresh/logout under the `permitAll` prefix `/auth-api/v1/auth/**`.

**Don't**
- **Don't** reintroduce a shared symmetric JWT secret, and never give a verifier the private key.
- **Don't** let one service read another service's datastore — the shared-DB coupling was removed on
  purpose.
- **Don't** trust `roles`/`restaurantCodes`/`customerId` from a request body for any privilege or
  ownership decision; force them server-side.
- **Don't** hardcode domain field names into the generic query/sync engines.
- **Don't** add an endpoint to a resource service without going through `core-security` (authN
  filter + authZ guards).
- **Don't** change the `issuer` claim, the CSV claim wire format, the `tokenType` semantics, or the
  public route prefix without a synchronized rollout to issuer **and** all verifiers.

---

## 6. Summary

Restaurant-OS is a credible early microservices platform: an asymmetric, well-documented security
model; services that no longer share a database; and a real CDC-driven search/analytics pipeline (the
two ES services are, in design terms, the strongest code in the repo). The event backbone is in place
to add the remaining domains — kitchen, payment, inventory, notification — as new sources onto the
same pipeline. The platform's current gaps and its forward plan are tracked internally in `DEFECTS.md`
and `ENHANCEMENTS.md`.
