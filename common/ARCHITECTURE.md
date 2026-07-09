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

## 3. Extensibility Rules — Dos & Don'ts

**Do**
- **Do** keep `common` a dependency-light leaf: only genuinely cross-cutting, domain- and
  security-free helpers belong here.
- **Do** reuse `JsonUtil`'s `ObjectMapper` where the same ISO-date JSON behaviour is needed.
- **Do** `clearFixedDateTime()` in test teardown whenever a test sets a fixed clock.

**Don't**
- **Don't** add any service's domain model or any auth/security concern here (those go to the owning
  service or `core-security`).
- **Don't** give `common` a project dependency — it must stay a leaf.
- **Don't** set `DateUtil`'s fixed time in production code paths.
