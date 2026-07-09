# order-service — Architecture

Owns the **order domain**: orders placed against a restaurant and their lifecycle. A verifier
service — it trusts JWTs minted by `identity-service` and enforces its own authorization (see
[`core-security/ARCHITECTURE.md`](../core-security/ARCHITECTURE.md)).

Runnable Spring Boot application (`bootJar`). Depends on:

- **`core-security`** — JWT verification, `@RestaurantCodes`, `ScopeGuard`, global 401/403 advice.

Local dev: port **8184**, **MySQL** `order_db` (`docker-compose` → `order-db`, `localhost:3308`),
JPA with `ddl-auto: update` (schema evolves from the entities — no migration tool yet).

It is also the **enrichment source** for the search pipeline: `elastic-service` calls back into
`/order-api/v1/order/search-documents/by-ids` (gated `hasRole('SYSTEM')`) to pull denormalized order
documents for indexing.

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
field (e.g. `customerId`) auto-applies.

---

## 5. Extensibility Rules — Dos & Don'ts

**Do**
- **Do** set `customerId` server-side from the caller's username, and keep the mapper's
  `@Mapping(ignore = true)` so it can never be spoofed from the DTO.
- **Do** validate every status change against `ALLOWED_TRANSITIONS` — the state machine is the
  authority for what moves are legal.
- **Do** call the matching `OrderAccessGuard` method on every endpoint (`assertStaffScope`,
  `assertCanView`, or `assertOwnerOrStaffScope`).
- **Do** keep the SYSTEM enrichment endpoint gated `hasRole('SYSTEM')` and owning the search-document
  shape.

**Don't**
- **Don't** trust `customerId` (or any ownership/scope field) from the request body.
- **Don't** add a status transition without also adding it to `ALLOWED_TRANSITIONS`.
- **Don't** add a mutating endpoint without both a `@PreAuthorize` role gate and an
  `OrderAccessGuard` check.

---

## 6. How to extend

- **New protected endpoint** — `@PreAuthorize` for the role tier; call the matching
  `OrderAccessGuard` method (`assertStaffScope` / `assertCanView` / `assertOwnerOrStaffScope`).
- **New status / transition** — add to `OrderStatus` and `ALLOWED_TRANSITIONS`; add an endpoint that
  sets it (staff-scoped like the others).
- **Let customers edit their own placed order** — allow `assertOwnerOrStaffScope` on `update` and
  restrict it to `PLACED` status.
