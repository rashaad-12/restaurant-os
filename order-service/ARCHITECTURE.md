# order-service — Architecture

Owns the **order domain**: orders placed against a restaurant and their lifecycle. A verifier
service — it trusts JWTs minted by `identity-service` and enforces its own authorization (see
[`core-security/ARCHITECTURE.md`](../core-security/ARCHITECTURE.md)).

Runnable Spring Boot application (`bootJar`). Depends on:

- **`core-security`** — JWT verification, `@RestaurantCodes`, `ScopeGuard`, global 401/403 advice.

Local dev: port **8184**, **MySQL** `order_db` (`docker-compose` → `order-db`, `localhost:3308`),
JPA with `ddl-auto: update` (schema evolves from the entities — no migration tool yet).

---

## 1. Domain

An **`Order`** (`orders` table) belongs to one restaurant (`restaurantCode`) and, when placed by a
customer, is **owned** by them (`customerId`). It has embedded `OrderItem`s and money fields.

| Field | Notes |
|---|---|
| `orderNumber` + `restaurantCode` | business key used for lookups/updates |
| `customerId` | **owner** — the username of the customer who placed it; `null` for staff-created orders. Set **server-side**, never from the request body |
| `status` | `OrderStatus` — see lifecycle below |
| `orderItems` | one-to-many, cascade all |
| `subtotal`/`tax`/`discount`/`deliveryFee`/`total`/`currency` | totals |

### Lifecycle (`OrderStatus`, transitions validated in `OrderServiceImpl.ALLOWED_TRANSITIONS`)

```
PLACED ──► ACCEPTED ──► PREPARING ──► COMPLETED
   │           └──────────────────► COMPLETED
   ├──► REJECTED
   └──► CANCELLED   (also from PREPARING)
```

A transition the map doesn't allow is silently skipped (the order is filtered out of the batch).

---

## 2. Component map

```
controller/OrderController       /order-api/v1/order — thin HTTP adapter, carries @PreAuthorize
service/OrderService(Impl)       bulk create + status transitions; ownership/scope checks
security/OrderAccessGuard        ownership (customerId) composed over core-security ScopeGuard
mapper/                          OrderMapper, OrderItemMapper, StatusMapper (MapStruct)
model/                           Order, OrderItem  (JPA entities)
dto/                             OrderDTO, OrderItemDTO
repository/OrderRepository       JPA; findByRestaurantCodeIn, findByCustomerId, findByOrderNumberAndRestaurantCode
enums/OrderStatus                PLACED | ACCEPTED | REJECTED | PREPARING | COMPLETED | CANCELLED
exception/                       OrderNotFoundException, local advice
config/SqlConfig                 @EnableJpaAuditing
```

---

## 3. Authorization — customer self-service + staff

This is the one service with **ownership**, not just tenant scope. `security/OrderAccessGuard`
composes core-security's `ScopeGuard`:

- `isOwner(order)` — `order.customerId == callerUsername()`
- `assertCanView(order)` — owner **or** `ScopeGuard.assertCanView(restaurantCode)` (staff in scope)
- `assertStaffScope(order)` — staff only: `ScopeGuard.assertWithinScope(restaurantCode)`
- `assertOwnerOrStaffScope(order)` — owner **or** staff in scope (used by cancel)
- `actingAsStaff()` — admin or a caller that has restaurant scope; otherwise the caller is a customer

**Two callers, one datastore:**
- A **customer** (role `CUSTOMER`, no restaurant scope) places orders they own and sees only their
  own (`findByCustomerId`). On create, `customerId` is forced to the caller's username — the DTO's
  `customerId` is ignored by the mapper (`@Mapping(ignore = true)`), so it can't be spoofed.
- **Staff** (roles with restaurant scope) manage the orders for their restaurant(s); `ADMIN` is global.

Role tier via `@PreAuthorize`; ownership/scope via `OrderAccessGuard` in the service (because
owner-or-staff and per-record scope can't be expressed as a role annotation). Denials → `403` via
core-security's global advice.

### Endpoints — `/order-api/v1/order`

| Method | Path | `@PreAuthorize` roles | Service check |
|---|---|---|---|
| POST | `/` create | ADMIN, OWNER, MANAGER, SERVER, CUSTOMER | customer → owns it; staff → `assertStaffScope` |
| GET | `/{id}` | authenticated | `assertCanView` (owner or staff-in-scope) |
| GET | `/getAll` | authenticated | admin → all; staff → their restaurants; customer → own |
| PUT | `/` update | ADMIN, OWNER, MANAGER, SERVER | `assertStaffScope` |
| PATCH | `/accept`,`/reject` | ADMIN, OWNER, MANAGER, COOK | `assertStaffScope` |
| PATCH | `/prepare` | ADMIN, MANAGER, COOK | `assertStaffScope` |
| PATCH | `/complete` | ADMIN, OWNER, MANAGER, SERVER, COOK | `assertStaffScope` |
| PATCH | `/cancel` | authenticated | `assertOwnerOrStaffScope` (owner may cancel own) |
| DELETE | `/` | ADMIN, OWNER, MANAGER | `assertStaffScope` |

---

## 4. Persistence

MySQL via Spring Data JPA (`orders` + `order_items`), `@EnableJpaAuditing` for
`createDttm`/`updateDttm`. Schema is currently created by Hibernate `ddl-auto: update`; adding a
field (e.g. `customerId`) auto-applies. **Note:** no Flyway yet, and `@Transactional` needs a real
transaction manager — bulk multi-row writes are not atomic across rows without one.

---

## 5. How to extend

- **New protected endpoint** — `@PreAuthorize` for the role tier; call the matching
  `OrderAccessGuard` method (`assertStaffScope` / `assertCanView` / `assertOwnerOrStaffScope`).
- **New status / transition** — add to `OrderStatus` and `ALLOWED_TRANSITIONS`; add an endpoint that
  sets it (staff-scoped like the others).
- **Let customers edit their own placed order** — allow `assertOwnerOrStaffScope` on `update` and
  restrict it to `PLACED` status.

---

## 6. Rating — 6 / 10

**Reviewed:** 2026-07-08 (source, not just design).

| Dimension | Score | Notes |
|---|---|---|
| Domain modelling | 8 / 10 | Best in the platform: explicit `ALLOWED_TRANSITIONS` state machine + server-side ownership. |
| Authorization | 8 / 10 | Ownership *and* tenant scope. `customerId` forced server-side and mapper-ignored → un-spoofable. `OrderAccessGuard` is unit-tested. |
| API contract | 4 / 10 | Bulk-only `List<OrderDTO>`, free-text `String` responses, **no `@Valid`**, no pagination, silent partial success. |
| Persistence correctness | 5 / 10 | JPA/MySQL with real transactions, but `ddl-auto: update` (no migrations), no `@Version`, N+1 lookups in bulk ops. |
| Code quality | 4 / 10 | Five near-identical status methods; copy-paste messages; the obscure no-op `peek`. |
| Testing | 5 / 10 | Solid `OrderAccessGuardTest` unit coverage, but **no** service/repository/web-layer integration tests (menu-service has these). |

**Verdict:** the domain design (ownership + state machine) is the sharpest in the repo, but the implementation is the least DRY and least tested of the two services. The gap is code hygiene, migrations, and concurrency safety — not the model.

### Known defects (fix before shipping)
| Sev | Finding | Location |
|---|---|---|
| 🟠 | No `@Version` optimistic lock — two staff transitioning the same order race; last-write-wins can skip a state or resurrect a cancelled order. | `Order` |
| 🟠 | No Bean Validation; malformed `OrderDTO` (null `orderNumber`, negative totals) is accepted. | `OrderController`, DTOs |
| 🟠 | `ddl-auto: update` in place of a migration tool — unsafe schema evolution in prod. | `application-localdev.yml` |
| 🟡 | Five near-identical methods (accept/prepare/complete/reject/cancel) — collapse to one `changeStatus(request, target)` guarded by `ALLOWED_TRANSITIONS`. | `OrderServiceImpl` |
| 🟡 | Copy-paste returns: prepare/complete/reject/cancel all say `"Order archive request…"`; `"No orders to publish"` everywhere. | `OrderServiceImpl` |
| 🟡 | Obscure no-op `.peek(o -> o.setOrderItems(o.getOrderItems()))` — relies on a setter side-effect to wire child back-refs; make it an explicit `wireItems()`. | `createOrder` |
| 🟡 | Invalid transitions are filtered *before* the scope check, and are silently dropped — caller gets a generic success and can't tell what was skipped. | `getOrderPairs`, all status ops |
| 🟡 | N+1: `getOrderPairs`/`deleteOrder` do one `findByOrderNumberAndRestaurantCode` per DTO. | `OrderServiceImpl` |

---

## 7. Next Plans (roadmap)

**P0 — Correctness & concurrency**
1. Add `@Version` to `Order` and handle `OptimisticLockException` → `409 Conflict`; this closes the status-transition race.
2. Introduce **Flyway** and switch `ddl-auto` to `validate`; baseline the current schema.
3. Add Bean Validation (`@Valid`, `@NotBlank orderNumber/restaurantCode`, `@NotNull status`, non-negative money) at the controller.
4. Collapse accept/prepare/complete/reject/cancel into a single guarded `changeStatus(List<OrderDTO>, OrderStatus target)`; fix all copy-paste messages; replace the no-op `peek` with an explicit wire step.

**P1 — Contract & lifecycle**
5. Return a structured `BulkResult` (created IDs, per-item outcome, skipped-with-reason) so silent partial success becomes explicit — surface invalid transitions as `422` per item.
6. Assert scope *before* transition-validity so an out-of-scope order returns `403`, not a silent skip (removes the mild existence-probe leak).
7. Emit order lifecycle events (`OrderPlaced/Accepted/Prepared/Completed/Cancelled`) to drive kitchen → payment → notification — the platform's next real integration; the SYSTEM search-enrichment endpoint already shows the CDC-notify pattern to reuse.
8. Pagination + filtering on `getAll` (by `status`, date range).

**P2 — Quality & data**
9. Server-side money integrity: recompute `subtotal/tax/total` from items (and a menu-price snapshot) rather than trusting request totals.
10. Batch the bulk lookups (`findByOrderNumberAndRestaurantCodeIn`) to kill the N+1.
11. Add service + web-layer integration tests (Testcontainers MySQL) to match menu-service's coverage.

---

## 8. Production Optimisation Plan

**Data & indexing**
- Add indexes on `(restaurant_code, status)` (kitchen queue reads), `customer_id` (customer order history), and unique `(order_number, restaurant_code)` (the business key used for every lookup/update).
- Fetch `orderItems` with a batch/`@BatchSize` or entity-graph to avoid per-order N+1 on list reads; consider a summary projection for `getAll`.

**Concurrency & throughput**
- Optimistic locking (P0 #1) over pessimistic — orders are low-contention per row; pair with idempotency keys on `createOrder` so client retries don't double-insert.
- HikariCP pool sizing tuned to MySQL `max_connections`; statement timeouts; `rewriteBatchedStatements=true` so `saveAll` issues real JDBC batches.
- Cap bulk request size to bound transaction time and lock windows.

**Resilience**
- When lifecycle events land (P1 #7): transactional outbox + retry/backoff so an order commit and its event publish can't diverge; circuit breakers/timeouts on any outbound call.

**Observability**
- Actuator is present — add Micrometer → Prometheus with per-transition timers/counters (`order.accept`, `order.complete`) and a gauge on open orders by status; OpenTelemetry tracing across the order → search enrichment hop.
- Structured JSON logs with `orderNumber` + `restaurantCode` + request-id in MDC.

**Config & security hardening**
- Externalise datasource URL/credentials to env/secrets (no `dev:dev` in committed yml); TLS to MySQL in prod.
- `ddl-auto: validate` (never `update`) in production; migrations own the schema.
- Disable devtools/livereload and drop `DEBUG` logging in the production profile.
