# menu-service — Architecture

Owns the **menu domain**: each restaurant's menus and their items. A verifier service in the
platform's security model — it trusts JWTs minted by `identity-service` and enforces its own
per-endpoint authorization (see [`core-security/ARCHITECTURE.md`](../core-security/ARCHITECTURE.md)).

Runnable Spring Boot application (`bootJar`). Depends on:

- **`core-security`** — JWT verification filter, `@RestaurantCodes`/`@CurrentUser`, `ScopeGuard`,
  the global 401/403 advice. Menu-service holds no keys and never issues tokens.
- **`common`** — `DateUtil` (publish timestamps).

Local dev: port **8183**, MongoDB `menu_db` (`docker-compose` → `menu-db`, `localhost:27018`).

---

## 1. Domain

A **`Menu`** (Mongo `menu` collection) belongs to one restaurant and is keyed by
`(restaurantCode, code)` — a unique compound index. It carries a `status` and a list of embedded
`MenuItem`s (each with variants).

| Field | Notes |
|---|---|
| `restaurantCode` | tenant key; the axis all authorization scopes on |
| `code` | menu code, unique within a restaurant |
| `status` | `DRAFT` → `PUBLISHED` → `ARCHIVED` |
| `items` | embedded `MenuItem[]` (variants embedded in turn) |
| `publishDttm` | set when a menu becomes `PUBLISHED` |

Writes are **bulk** (`List<MenuDTO>`); items are merged on update (`MenuItemDTO.markedForDeletion`
removes, matching `code` updates, else adds).

---

## 2. Component map

```
controller/MenuController        /menu-api/v1/menu — thin HTTP adapter, carries @PreAuthorize
service/MenuService(Impl)        bulk create/update/publish/archive/delete + reads; scope checks
mapper/                          MenuMapper, MenuItemMapper, MenuItemVariantMapper (MapStruct)
model/                           Menu, MenuItem, MenuItemVariant  (Mongo documents)
dto/                             MenuDTO, MenuItemDTO, MenuItemVariantDTO
repository/MenuRepository        Mongo; findByRestaurantCodeIn, findByCodeAndRestaurantCode, …
enums/MenuStatus                 DRAFT | PUBLISHED | ARCHIVED
exception/                       MenuNotFoundException, InvalidMenuStateException, local advice
config/MongoConfig               @EnableMongoAuditing
```

---

## 3. Authorization

Restaurant-scoped and staff-managed. Two layers (the platform pattern — see
`core-security/ARCHITECTURE.md` §8):

- **Role gating** — `@PreAuthorize("hasAnyRole('ADMIN','OWNER','MANAGER')")` on every write.
- **Tenant scope** — `MenuServiceImpl` calls `ScopeGuard.assertWithinScope(menu.restaurantCode)` on
  each write payload, and `ScopeGuard.assertCanView(...)` on `getMenuById`. `ADMIN` is global.

Reads are scoped to the caller's tenant: `getAll`/`published` take `@RestaurantCodes` (the caller's
codes from the token) and query only those; `getMenuById` is view-checked. A caller with no
restaurant scope (e.g. a customer) sees nothing and gets `403` on a point read.

Denials → `AccessDeniedException` → `403` JSON via core-security's `GlobalSecurityExceptionHandler`.

### Endpoints — `/menu-api/v1/menu`

| Method | Path | Roles | Scope |
|---|---|---|---|
| GET | `/{id}` | authenticated | `assertCanView` |
| GET | `/getAll` | authenticated | caller's `@RestaurantCodes` |
| GET | `/published` | authenticated | caller's `@RestaurantCodes` (PUBLISHED only) |
| POST | `/` | ADMIN, OWNER, MANAGER | `assertWithinScope` |
| PUT | `/` | ADMIN, OWNER, MANAGER | `assertWithinScope` |
| PATCH | `/publish` | ADMIN, OWNER, MANAGER | `assertWithinScope` |
| PATCH | `/archive` | ADMIN, OWNER, MANAGER | `assertWithinScope` |
| DELETE | `/` | ADMIN, OWNER, MANAGER | `assertWithinScope` |

---

## 4. Persistence

MongoDB only (`menu_db`, `menu` collection). Unique compound index on `(restaurantCode, code)`;
`@EnableMongoAuditing` fills `createDttm`/`updateDttm`. Local URI in `application-localdev.yml`;
production via `MONGODB_URI`.

---

## 5. How to extend

- **New protected endpoint** — add `@PreAuthorize` for the role tier, and call
  `ScopeGuard.assertWithinScope` (writes) / `assertCanView` (reads) in the service for the tenant.
- **Customer-facing published menus** — today `published` is staff-scoped; a public read for diners
  would be a separate endpoint that takes the restaurant code as input rather than from the token.

---

## 6. Rating — 6.5 / 10

**Reviewed:** 2026-07-08 (source, not just design).

| Dimension | Score | Notes |
|---|---|---|
| Layering & structure | 8 / 10 | Clean `Controller → Service → Repository`, MapStruct at every boundary, local exception advice. |
| Authorization | 8 / 10 | Two-layer model done right: `@PreAuthorize` role gate + `ScopeGuard.assertWithinScope` on every write, `assertCanView` on point reads. Tenant axis derived from token, not body. |
| API contract | 4 / 10 | Bulk-only `List<MenuDTO>`, free-text `String` responses, **no `@Valid`**, no pagination, verb-in-path (`/getAll`). |
| Persistence correctness | 4 / 10 | `@Transactional` on single-node Mongo is silently non-atomic; unused **Flyway** dependency can't run against Mongo. |
| Code quality | 6 / 10 | Readable and DRY-ish, but copy-paste `"No menus to publish"` in update/archive/delete, and a missing `continue` in the merge-deletion path. |
| Testing | 8 / 10 | Testcontainers integration tests with a rich `test-data/` fixture tree (create/update/publish/archive workflows). Best-tested service in the repo. |

**Verdict:** the most polished domain service in the platform — strong authz and the only one with real end-to-end tests. The gap to production is API-contract discipline and Mongo transaction semantics, not architecture.

### Known defects (fix before shipping)
| Sev | Finding | Location |
|---|---|---|
| 🟠 | `@Transactional` requires a Mongo replica set; single-node compose can't honor it → multi-doc writes are non-atomic. | `MenuServiceImpl` |
| 🟠 | No Bean Validation anywhere; malformed `MenuDTO` (null `code`/`restaurantCode`) reaches Mongo. | `MenuController`, DTOs |
| 🟡 | Copy-paste return message `"No menus to publish"` in `updateMenu`/`archiveMenu`. | `MenuServiceImpl` |
| 🟡 | `updateAndMergeMenuItems` doesn't `continue` after a `markedForDeletion` match — runs a wasted `updateEntityFromDTO` on the removed item. | `MenuServiceImpl.updateAndMergeMenuItems` |
| 🟡 | `getMenuByCodeAndRestaurantCode` performs no scope check — safe only because it's unexposed; a landmine if wired to a controller. | `MenuServiceImpl` |
| 🟡 | Unused `flyway-core` on a Mongo-only service — dead/ misleading dependency. | `build.gradle` |
| 🟡 | Bulk writes silently swallow partial failures; caller gets a generic success string with no per-item outcome or IDs. | `MenuController`, `MenuServiceImpl` |

---

## 7. Next Plans (roadmap)

**P0 — Contract & correctness**
1. Add Bean Validation (`@Valid` + `@NotBlank code/restaurantCode`, `@NotNull status`, `@Positive price`) to `MenuDTO`/`MenuItemDTO`; reject bad payloads at the edge.
2. Replace free-text `String` returns with a `BulkResult { processed, skipped, perItem[] }` DTO carrying created/updated IDs and per-item status.
3. Resolve Mongo transactions: either run Mongo as a single-node **replica set** (`--replSet`) so `@Transactional` is honored, or drop `@Transactional` where cross-doc atomicity isn't actually needed and document the choice.
4. Fix the copy-paste `"No menus to publish"` messages and add the missing `continue` in the merge path.

**P1 — Domain & reads**
5. Public diner-facing read: a `GET /public/{restaurantCode}/published` that takes the code as input (not from token) for customer apps, cached and rate-limited at the gateway.
6. Pagination + filtering on `getAll` (`Pageable`, filter by `status`/`category`).
7. Emit domain events on publish/archive (menu changed) so downstream (search index, caching, order-service price snapshots) stays consistent — reuse the platform's CDC-notify pattern already used for ES sync.

**P2 — Quality**
8. Extract the repeated `find→filter→mutate→saveAll` shape (publish/archive/delete) into one guarded helper.
9. Drop the unused Flyway dependency (or replace with a Mongock changelog if schema/seed migrations become real).
10. Contract test the authz matrix (role × scope) alongside the existing workflow tests.

---

## 8. Production Optimisation Plan

**Data & indexing**
- Keep the unique compound index `(restaurantCode, code)`; add a secondary index on `(restaurantCode, status)` to back `findByRestaurantCodeInAndStatus` (the hot published-menu read).
- Cap embedded `items[]` growth (menus with hundreds of items) — consider projecting item summaries for list reads and fetching full items only on point reads.

**Caching**
- Published menus are read-heavy and change rarely: front them with a Redis (or gateway) cache keyed by `(restaurantCode, code)`, invalidated on publish/archive events (P1 #7). This is the single biggest latency/DB-load win.

**Resilience & limits**
- Cap bulk request size (e.g. ≤100 menus/request) to bound memory and write amplification.
- Mongo client: explicit connection-pool sizing, socket/connect timeouts, and read preference; retryable writes on.

**Observability**
- Actuator is already present — wire Micrometer → Prometheus, expose `menu.publish`/`menu.update` timers and counters, and add OpenTelemetry tracing so a publish that fans out to search is traceable end-to-end.
- Structured JSON logging with `restaurantCode` + request-id in MDC.

**Config & security hardening**
- Externalise `MONGODB_URI` and credentials to env/secrets (no `dev:dev` in committed yml); enable TLS to Mongo in prod.
- Turn off `spring.data.mongodb.auto-index-creation` in production and manage indexes explicitly (avoid surprise index builds under load).
- Disable devtools/livereload and `DEBUG` logging in the production profile.
