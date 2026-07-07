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
