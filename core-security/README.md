# core-security

Shared security library for the platform. It provides **stateless JWT authentication** —
issuing, verifying, the Spring Security filter chain, `@CurrentUser` / `@RestaurantCodes`
resolvers, cookie handling, and JSON error responses — to every service that depends on it.

It is a **plain library** (`jar`, not `bootJar`): it has no `main` and starts nothing on its own.
It only contributes beans and configuration into a host Spring Boot application.

For the full design, invariants, and extension guide see [`ARCHITECTURE.md`](./ARCHITECTURE.md).

---

## Using it from another service

```gradle
// <service>/build.gradle
dependencies {
    implementation project(':core-security')
}
```

Component-scan `com.restaurantos` so the library's beans are picked up — services already do:

```java
@SpringBootApplication(scanBasePackages = "com.restaurantos")
```

A **verifier** (any service other than auth-service) needs **no `app.security.jwt` configuration
at all**: the public key ships inside this jar (`classpath:keys/app_public.pem`) and every other
value has a production-safe default. Just import the module and scan the package.

---

## Local setup

### Prerequisites
- JDK 17 (the Gradle toolchain pins Java 17)
- OpenSSL (for the dev key script)
- Build from the repository root using the Gradle wrapper (`./gradlew`, or `gradlew.bat` on
  Windows). There is no per-module wrapper.

### 1. Generate the dev keypair (once after cloning)

The RSA keypair is **not committed** (`.gitignore` → `**/keys/*.pem`). Generate it once; the
script writes the **public** key into this module and the matching **private** key into
`auth-service`:

```bash
sh scripts/generate-dev-keys.sh
```

This creates `core-security/src/main/resources/keys/app_public.pem`. Without it, any consumer
service fails at startup with *"Key resource not found on classpath: keys/app_public.pem"*.

### 2. Build / test

```bash
# from the repository root
./gradlew :core-security:build      # compile + test + jar
./gradlew :core-security:test       # tests only
```

Because this is a library, you don't "run" it — you run a consumer service (e.g. `auth-service`
or `user-service`) that depends on it. Verify it end-to-end by starting a consumer and hitting an
authenticated endpoint.

---

## Configuration reference (host application supplies these)

All under `app.security.jwt.*`; all optional for a verifier.

| Property | Default | Notes |
|---|---|---|
| `issuer` | `restaurant-os` | must be identical across all services |
| `public-key` | `classpath:keys/app_public.pem` | inline PEM, `classpath:`, or file path |
| `private-key` | *(none)* | **issuer only** — do not set on a verifier |
| `clock-skew` | `PT30S` | tolerated issuer/verifier clock drift |
| `ttl.<audience>.access-token-ttl` / `.refresh-token-ttl` | STAFF 15m/1d · PARTNER 30m/7d · CUSTOMER 30m/30d | issuer only |
| `cookie.secure` / `cookie.same-site` / `cookie.path` / `cookie.domain` | `true` / `Strict` / `/` / — | issuer only |
| `cors.enabled` / `cors.allowed-origins` / … | `false` / `[]` / … | browser-facing services only |

In production, inject the private key without touching code, e.g.
`APP_SECURITY_JWT_PRIVATE_KEY=<inline PEM>` on auth-service only.

---

## What you get for free (in a consumer)

```java
@GetMapping("/orders")
public List<Order> myOrders(@CurrentUser AuthenticatedUser user,
                            @RestaurantCodes Set<String> codes) {
    // user + codes are read from the verified token; unauthenticated calls are rejected 401
}

@PreAuthorize("hasRole('MANAGER')")   // roles arrive as ROLE_* authorities
@PostMapping("/menu")
public void addItem(...) { }
```

Public by default: `/auth-api/v1/auth/**` and `/actuator/health/**`. Everything else requires a
valid access-token cookie or `Authorization: Bearer <token>`.
