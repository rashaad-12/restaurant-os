# common — Architecture

A tiny, dependency-light **shared utility library** (`jar`, not `bootJar` — no `main`, starts
nothing). It holds cross-cutting helpers that are not domain- or security-specific, so any service
can depend on it without pulling in a domain. Security primitives live in `core-security`, not here.

Consumed today by **menu-service** (publish timestamps) and available to any module. A consumer adds
`implementation project(':common')`.

---

## 1. Contents

```
util/
  DateUtil        single clock seam: getCurrentDateTime(); test-overridable fixed time
  JsonUtil        Jackson helpers: readFromFile(classpath, Class|TypeReference), writeValueAsString
enums/
  DateConstants   shared date/time format constants
```

| Type | Purpose | Why it's shared |
|---|---|---|
| `DateUtil` | One place the platform reads "now" (`getCurrentDateTime`), overridable via `setFixedDateTime` for deterministic tests. | Publish/audit timestamps must be mockable; menu-service's Testcontainers fixtures assert exact `publishDttm`. |
| `JsonUtil` | Read a classpath JSON resource into a type, and serialise objects to JSON, using one pre-configured `ObjectMapper` (`JavaTimeModule`, ISO dates not epoch). | Test-fixture loading and consistent JSON date handling across services. |
| `DateConstants` | Canonical date/time format strings. | Avoids each service re-declaring format literals. |

---

## 2. Design notes & invariants

1. **No domain, no security.** `common` must stay free of any service's model and of auth concerns
   (those belong to the owning service and to `core-security`). Keep it a leaf with no project
   dependencies.
2. **`DateUtil` is a test seam, not app state.** The static `fixedDateTime` exists for tests;
   production never sets it. Always `clearFixedDateTime()` in test teardown — a leaked fixed clock
   would freeze time for the whole JVM.
3. **`JsonUtil`'s `ObjectMapper` is the shared JSON contract** (ISO-8601 dates, `JavaTimeModule`).
   Services that need the same behaviour should reuse it rather than hand-rolling a mapper.

---

## 3. Known issues / cleanup

Severity: 🟠 major · 🟡 minor

| Sev | Finding | Detail |
|---|---|---|
| 🟠 | **Over-heavy dependency.** `build.gradle` pulls `spring-boot-starter-web` **and** `spring-web` into a plain util jar, dragging the full servlet/web stack onto every consumer's classpath. `JsonUtil`/`DateUtil` need only `jackson-databind` + `jackson-datatype-jsr310`. | `common/build.gradle` |
| 🟡 | **Static-state utilities.** `DateUtil` and `JsonUtil` are static holders; a `Clock` bean and an injected `ObjectMapper` would be more testable and align with the constructor-injection style used everywhere else. | `DateUtil`, `JsonUtil` |
| 🟡 | **Duplicate `ObjectMapper` config.** `JsonUtil`, `core-security`'s JSON, and each ES service's `JacksonJsonpMapper` each build their own mapper with the same `JavaTimeModule` + no-timestamps setup. One shared configuration would remove the drift. | platform-wide |
| 🟡 | Stray double semicolon in `JsonUtil` field initialiser. | `JsonUtil` |

---

## 4. Next plans

- **Trim to Jackson only.** Replace the web starters with `jackson-databind` + `jackson-datatype-jsr310`
  (`implementation`), so `common` stops imposing the web stack on libraries and non-web consumers.
- **Expose a shared `ObjectMapper`/`Clock`.** Offer a small auto-configuration (or plain factory) so
  services reuse one JSON + clock configuration instead of duplicating it.
- **Absorb genuinely shared cross-cutting types** as they emerge (e.g. a platform-wide `ApiResponse`
  /`BulkResult` envelope, once services stop returning free-text `String`s) — but keep the module
  domain- and security-free.

---

## 5. Production notes

`common` ships no runtime behaviour of its own — it is compiled into its consumers — so it has no
independent production surface. The only production-relevant action is dependency hygiene (§3/§4):
keeping its transitive footprint minimal so it can't drag unwanted libraries or CVEs into every
service that depends on it.
