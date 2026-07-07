# identity-service

The platform's **identity provider and sole JWT issuer**, and the owner of the **user &
restaurant domain**. It authenticates staff (internal password), customers, and delivery partners
(OAuth / OIDC), mints the RS256 access + refresh tokens that every other service verifies, and
serves the user/restaurant CRUD APIs.

> Formed by merging the former `auth-service` and `user-service`. They already shared the `user_db`
> MongoDB (auth-service depended on `user-service`'s `UserRepository`), so keeping them as two
> deployables bought coupling with no isolation. One module removes that seam.

Surfaces:

| Area | Route prefix |
|---|---|
| Authentication (staff / customer / partner) | `/auth-api/v1/auth/**` |
| Users | `/user-api/v1/user` |
| Restaurants | `/restaurant-api/v1/restaurant` |

- Design, flows, and invariants: [`ARCHITECTURE.md`](./ARCHITECTURE.md)
- Token/verification model shared across services: [`../core-security/ARCHITECTURE.md`](../core-security/ARCHITECTURE.md)

Runs on port **8181** in local dev.

---

## Prerequisites

- **JDK 17** (Gradle toolchain pins Java 17)
- **OpenSSL** (for the dev key script)
- **Docker + Docker Compose** (for the database) — or a local MongoDB
- Build from the repository root with the Gradle wrapper (`./gradlew`, or `gradlew.bat` on
  Windows).

---

## Data store it talks to

| Store | Used for | Local source |
|---|---|---|
| **MongoDB** (`user_db`) | reads/writes `User` and `Restaurant` records (`UserRepository` / `RestaurantRepository`) — the identity store this service owns | `docker-compose` → `user-db` (host port **27019**) |

identity-service runs no relational datastore. `application-localdev.yml` already points
`spring.data.mongodb.uri` at `user_db` on `localhost:27019`.

---

## Local setup

### 1. Generate the signing keypair (once after cloning)

`identity-service` signs JWTs with an RSA **private** key; all other services verify with the matching
**public** key (loaded from `core-security` at `classpath:keys/app_public.pem`).

**Neither key is committed** — they are a per-environment pair (`.gitignore` → `**/keys/*.pem`),
and a private key must never be in source control.

```bash
sh scripts/generate-dev-keys.sh
```

This writes:
- `identity-service/src/main/resources/keys/app_private.pem` — private signing key (this module)
- `core-security/src/main/resources/keys/app_public.pem` — public verification key (all services)

Re-run any time to rotate your local pair.

### 2. Start the database

```bash
# from the repository root
docker compose up -d user-db
```

`user-db` (Mongo) → `localhost:27019`, user `dev` / `dev`, database `user_db`. The default
`localdev` config already targets it — no further wiring needed.

### 3. Run

```bash
# from the repository root
./gradlew :identity-service:bootRun --args='--spring.profiles.active=localdev'
```

The service starts on `http://localhost:8181`. On startup `core-security` logs
`JwtService initialised (issuer=restaurant-os, signing=enabled)` — `signing=enabled` confirms the
private key was loaded.

### 4. Smoke test

Staff login (needs an existing user in `user_db` with a BCrypt password — seed one via the
`/user-api/v1/user` endpoint on this service):

```bash
curl -i -X POST http://localhost:8181/auth-api/v1/auth/internal/login \
  -H 'Content-Type: application/json' \
  -d '{"audience":"STAFF","username":"alice@example.com","password":"secret"}'
```

A `200` with `Set-Cookie: accessToken=...; refreshToken=...` means the full sign path works. Use
those cookies against any protected endpoint on another service to confirm end-to-end verification.

---

## OAuth (customer / partner) local testing

OAuth login verifies a **real OIDC ID token** from a configured provider — there is no mock path.
Configure at least one provider's client IDs and post a genuine ID token:

```bash
curl -i -X POST http://localhost:8181/auth-api/v1/auth/customer/login \
  -H 'Content-Type: application/json' \
  -d '{"authProvider":"google","idToken":"<real OIDC id_token>"}'
```

- Provider registry: `app.oauth.providers.<name>` (issuer + `audiences` = your client IDs).
- To let first-time users be created automatically, set `app.oauth.provisioning.enabled=true`
  (off by default; partners are provisioned as `PENDING_APPROVAL` and cannot get tokens until
  activated).

---

## Production

Do **not** ship keys in the image. Inject them at runtime from your secret manager; everything in
`application.yml` is env-driven:

```
APP_SECURITY_JWT_PRIVATE_KEY=<inline PEM>     # identity-service only (issuer)
APP_SECURITY_JWT_PUBLIC_KEY=<inline PEM>      # all services (or bundle / JWKS)
MONGODB_URI=<mongodb connection string>       # shared user store
COOKIE_SECURE=true  COOKIE_SAME_SITE=Strict   # HTTPS cookie policy
GOOGLE_CLIENT_IDS=...  APPLE_CLIENT_IDS=...    # OAuth audiences per provider
```

Or point `app.security.jwt.private-key` / `public-key` at a mounted secret path.
