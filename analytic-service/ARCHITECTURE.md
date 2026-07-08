# analytic-service — Architecture

The platform's **read-only analytics / search API over Elasticsearch**. It exposes a **generic,
schema-driven** query + aggregation surface over the per-tenant order indices that `elastic-service`
maintains (`dev_orders_<org>`). It owns no data and does no writes — it turns a declarative
`SearchCriteria` (a nestable condition tree + sort + pagination + aggregations) into a native
Elasticsearch request, executes it, and maps the result back.

Runnable Spring Boot application (`bootJar`). Local dev: port **8186**. Reads Elasticsearch directly
via the ES Java client. **It is the query side of the CDC pipeline** — the counterpart to
elastic-service's write side.

Depends on: `co.elastic.clients:elasticsearch-java`, Jackson. **No datastore of its own.**

> ⚠️ **Security gap (see §6).** This service currently has **no authentication or authorization** —
> no `core-security`, no `spring-boot-starter-security`, no `@PreAuthorize`, and `orgCode` is taken
> straight from the URL path. Any caller can query any tenant's analytics. This is the #1 item to fix
> before it is exposed beyond a trusted network.

---

## 1. Where it sits

```
  Elasticsearch  dev_orders_<org>          ◄── written by elastic-service (CDC sync)
         ▲
         │ native ES query / aggregation (read-only)
         │
   ┌───────────────────── analytic-service ─────────────────────┐
   │ AnalyticsController   POST /api/analytics/orders/{org}/…    │
   │ AnalyticsServiceImpl  resolve index → field caps → build →  │
   │                       execute → map                          │
   │ MappingFieldRegistry  ES field-caps introspection (cached)  │
   │ SearchRequestBuilder  query + sort + paging + aggs + limits │
   │   QueryBuilder        Condition tree → ES bool/nested query │
   │   AggregationBuilder  AggregationCriteria → ES aggregations │
   │ AggregationParser     ES agg response → AggregationResult   │
   │ ESSearchRepository    thin execute-only ES boundary         │
   └─────────────────────────────────────────────────────────────┘
```

---

## 2. Request model — a generic query language

The API is one endpoint pair that accepts a declarative `SearchCriteria`, so clients express
arbitrary filters without the service hard-coding domain fields:

```
SearchCriteria {
  page, size,
  filter: Condition,               // recursive AND/OR tree of leaves
  sort:   [ SortCriteria ],
  aggregations: [ AggregationCriteria ]   // nestable metric/bucket aggs
}

Condition (leaf)  { field, operator, values[], valueCondition }   // e.g. status TERM [PLACED,ACCEPTED]
Condition (group) { conditionType: AND|OR, conditions: [ Condition ] }
```

`FieldOperator` maps to ES operators: `TERM`, `CONTAINS`/`PREFIX`/`SUFFIX` (match vs. wildcard by
field type), `EXISTS`, `RANGE_GT/GTE/LT/LTE/BETWEEN`, plus negation. `FieldType` (`TEXT`, `KEYWORD`,
`DATE`, `LONG`, `DOUBLE`, `BOOLEAN`) drives per-type query construction (e.g. `match` on `TEXT` vs.
`term` on `KEYWORD`, typed range on `DATE`/number).

### Endpoints — `/api/analytics/orders`

| Method | Path | Body | Returns |
|---|---|---|---|
| POST | `/{orgCode}/search` | `SearchCriteria` | `SearchResult<ObjectNode>` — `{total, page, size, items[]}` |
| POST | `/{orgCode}/aggregate` | `SearchCriteria` (optional) | `AggregationResult` — `{total, aggregations}` |

---

## 3. How a request is served

1. **Resolve index.** `IndexResolver.forOrg(orgCode)` → `prefix + org` (lower-cased), e.g.
   `dev_orders_r1`. `orgCode` blank → `400`.
2. **Introspect the schema.** `MappingFieldRegistry.forIndex(index)` calls ES **field-caps** (`*`)
   and builds a `FieldTypes` (field → `FieldType`, plus the set of `nested` paths), cached per index
   in a `ConcurrentHashMap`. Unknown/uncached indices default fields to `KEYWORD` and log a warning
   (query still runs). This is what makes the query builder schema-driven rather than field-hardcoded.
3. **Build the request.** `SearchRequestBuilder` assembles a native `SearchRequest`:
   - `QueryBuilder` turns the `Condition` tree into an ES `bool` query — AND→`filter`/`must`,
     OR→`should` + `minimumShouldMatch(1)`, negation→`mustNot`, and **nested fields** (e.g. order
     items) grouped under `nested` queries per path with `ChildScoreMode.None`.
   - sort, `from = page*size`, `size` (clamped), aggregations, and per-query `timeout`.
4. **Enforce guardrails** (`QueryLimits`, see §4) — reject deep pagination and oversized/over-deep
   aggregation trees **before** hitting ES.
5. **Execute.** `ESSearchRepository.search(request, type)` — a deliberately thin boundary that only
   runs the pre-built request (`ObjectNode` for search hits, `Void` for aggregate-only).
6. **Map back.** Search → inject `_id` as `id` when absent, page math, total. Aggregate →
   `AggregationParser` walks the ES aggregation response against the requested specs into a
   structured `AggregationResult` (buckets/metrics).

**Design intent:** the repository does nothing but execute; all assembly (query, limits, sort, aggs)
and all response mapping live in the query/service layer, keeping the ES boundary trivially testable
and swappable.

---

## 4. Guardrails — a generic query surface is a DoS foot-gun

`QueryLimits` (`analytics.limits.*`) bounds the otherwise-unbounded query surface:

| Limit | Default | Enforced |
|---|---|---|
| `maxPageSize` | 100 | `size` clamped down |
| `maxResultWindow` | 10 000 | `from + size` over this → `400` ("narrow the query instead of deep-paging") |
| `maxAggregations` | 20 | total aggregation count (recursive) over this → `400` |
| `maxAggregationDepth` | 3 | aggregation nesting deeper than this → `400` |
| `queryTimeoutMs` | 5 000 | applied as the ES request `timeout` |
| `maxTermsSize` | 1 000 | (config present; wire into terms aggs) |

`IllegalArgumentException` → `400` via `CustomExceptionHandler`; other runtime failures → `500`.

---

## 5. Invariants

1. **Read-only, owns no data.** No writes to ES, no datastore. The index contents are owned by the
   source service via elastic-service; analytic-service is a pure query layer.
2. **Schema-driven, not field-hardcoded.** Query construction is derived from live field-caps, so the
   same code serves any index shape. Don't hardcode domain field names into the builders.
3. **Every query is bounded.** All pagination/aggregation limits in `QueryLimits` must stay enforced
   before execution — this is the DoS guard for a generic API.
4. **The repository only executes.** Keep request assembly and response mapping out of
   `ESSearchRepository`.
5. **Tenant isolation is by index.** A request only ever targets `prefix + orgCode`. Once authz is
   added (§6/§7), the caller's token scope — not the path `orgCode` — must decide which org they may
   read.

---

## 6. Rating — 6.5 / 10

**Reviewed:** 2026-07-08 (source).

| Dimension | Score | Notes |
|---|---|---|
| Query engine design | 9 / 10 | Genuinely strong: schema-driven via field-caps, recursive bool/nested query builder, type-aware operators, clean builder/executor split. |
| Guardrails | 8 / 10 | Thoughtful DoS bounds (result window, agg count/depth, timeout) enforced pre-execution — rare and correct. |
| Layering | 8 / 10 | Controller → service → builders → execute-only repo; response mapping isolated. |
| **Security** | **1 / 10** | **No authN/authZ at all**; `orgCode` from the path → any caller reads any tenant. Ships as an open cross-tenant data endpoint. |
| Robustness | 6 / 10 | `IndexResolver`'s `@Value` on a `final` field with a constructor is redundant/fragile; blanket `RuntimeException`→500 leaks messages; no `@Valid`. |
| Testing | 2 / 10 | No tests over the query builder — the most logic-dense, highest-value code to cover. |

**Verdict:** technically the most impressive piece of engineering in the repo — a real generic ES
query engine with proper guardrails — but it is **not shippable as-is** because it has no auth. The
engine is production-quality; the perimeter is missing.

### Known gaps
| Sev | Finding |
|---|---|
| 🔴 | **No authentication/authorization.** No `core-security`, no `@PreAuthorize`; `orgCode` is an unauthenticated path param → cross-tenant read of any restaurant's orders. |
| 🟠 | No tests for `QueryBuilder`/`AggregationBuilder`/`AggregationParser` (the core value). |
| 🟠 | Committed ES credentials in `application-localdev.yml`; plaintext ES. |
| 🟡 | `IndexResolver` mixes `@Value` on a `final` field **and** a constructor param — confusing; pick constructor binding. |
| 🟡 | `handleRuntime` returns raw exception messages as `500` bodies — information leak; map to a generic error. |
| 🟡 | No request validation (`@Valid`) on `SearchCriteria`; `maxTermsSize` defined but not wired. |

---

## 7. Next plans (roadmap)

**P0 — Close the security hole**
1. Add `core-security` + `spring-boot-starter-security`; require an authenticated token on both
   endpoints.
2. Derive the tenant from the **token scope**, not the path: reject (or ignore) a path `orgCode` the
   caller isn't scoped to — reuse `ScopeGuard.assertCanView(org)`. `ADMIN` reads any; staff read
   their `restaurantCodes`; customers get nothing (or only their own via a future filter).
3. Add tests covering the authz matrix (in-scope / out-of-scope / admin).

**P1 — Correctness & robustness**
4. Unit-test the query engine: AND/OR/negation, nested-path grouping, per-type operators, range/date
   handling, and the guardrail rejections (deep paging, agg count/depth).
5. `@Valid` on `SearchCriteria`; sanitise the `500` handler so internal messages aren't returned.
6. Fix `IndexResolver` construction; wire `maxTermsSize` into terms aggregations.

**P2 — Capability & performance**
7. Field allow-list / alias layer so clients query stable logical field names, decoupled from index
   mappings (and to prevent querying sensitive fields).
8. Response caching for hot dashboards (short-TTL, keyed by org + criteria hash).
9. `search_after` cursor pagination as the sanctioned path past `maxResultWindow` for exports.
10. Broaden beyond orders (menus, etc.) once those indices exist — the engine is already generic.

---

## 8. Production optimisation plan

**Query performance**
- Prefer `filter`/`term` context (non-scoring, cacheable) over `must`/`match` wherever scoring isn't
  needed — the builder already routes exact-match to `filter`; keep it that way.
- Keep `trackTotalHits` honest but consider capping it (e.g. `track_total_hits: 10000`) for large
  indices where an exact count is expensive and unused.
- Enforce the query `timeout` (present) and add `terminate_after` for pathological queries.

**Field-caps caching**
- `MappingFieldRegistry` caches per index indefinitely — add TTL/invalidation so mapping changes
  (new fields) are picked up without a restart, and pre-warm the cache for known tenants on startup.

**Connection & resource tuning**
- ES client: bounded connection pool, explicit connect/socket timeouts (present), retry on 429/503,
  and back-pressure when ES sheds load. Run read traffic against ES replica shards.

**Scaling & isolation**
- Stateless service → scale horizontally behind the gateway; enforce per-tenant rate limits at the
  edge (a generic query API invites expensive requests).
- Keep the `QueryLimits` guardrails authoritative in every environment; tune them per cluster size.

**Security & config hardening**
- Ship the P0 auth first — do not expose this service publicly until tenant scope is enforced from
  the token.
- Externalise ES credentials to env/secrets; enable TLS + auth to Elasticsearch; drop `DEBUG` and
  devtools in the production profile.
