# analytic-service — Architecture

The platform's **read-only analytics / search API over Elasticsearch**. It exposes a **generic,
schema-driven** query + aggregation surface over the per-tenant order indices that `elastic-sync-service`
maintains (`dev_orders_<org>`). It owns no data and does no writes — it turns a declarative
`SearchCriteria` (a nestable condition tree + sort + pagination + aggregations) into a native
Elasticsearch request, executes it, and maps the result back.

Runnable Spring Boot application (`bootJar`). Local dev: port **8186**. Reads Elasticsearch directly
via the ES Java client. **It is the query side of the CDC pipeline** — the counterpart to
elastic-sync-service's write side.

Depends on: `co.elastic.clients:elasticsearch-java`, Jackson. **No datastore of its own.**

---

## 1. Where it sits

```
  Elasticsearch  dev_orders_<org>          ◄── written by elastic-sync-service (CDC sync)
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
   source service via elastic-sync-service; analytic-service is a pure query layer.
2. **Schema-driven, not field-hardcoded.** Query construction is derived from live field-caps, so the
   same code serves any index shape. Don't hardcode domain field names into the builders.
3. **Every query is bounded.** All pagination/aggregation limits in `QueryLimits` must stay enforced
   before execution — this is the DoS guard for a generic API.
4. **The repository only executes.** Keep request assembly and response mapping out of
   `ESSearchRepository`.
5. **Tenant isolation is by index.** A request only ever targets `prefix + orgCode`. Once
   authorization is in place, the caller's **token scope** — not the path `orgCode` — must decide
   which org they may read.

---

## 6. Extensibility Rules — Dos & Don'ts

**Do**
- **Do** keep query construction driven by live field-caps so the engine stays domain-agnostic.
- **Do** enforce every `QueryLimits` bound *before* the request reaches Elasticsearch.
- **Do** keep `ESSearchRepository` a pure execute-only boundary — build and map elsewhere.
- **Do** target exactly one tenant index per request (`prefix + orgCode`).
- **Do** prefer `filter`/`term` (non-scoring, cacheable) context over `must`/`match` where scoring
  isn't needed — the builder already routes exact-match to `filter`; keep it that way.

**Don't**
- **Don't** hardcode domain field names into `QueryBuilder`/`AggregationBuilder`.
- **Don't** put request assembly or response mapping into the repository.
- **Don't** widen or bypass a guardrail without a matching new bound — a generic query API invites
  expensive requests.
- **Don't** let a tenant read an index it isn't scoped to once authorization exists (derive the org
  from the token, not the path).

---

## 7. How to extend

- **New query operator / field type** — extend `FieldOperator`/`FieldType` and the per-type branch in
  `QueryBuilder`; the schema-driven dispatch does the rest.
- **New aggregation kind** — add it to `AggregationCriteria` handling in `AggregationBuilder` and the
  matching walk in `AggregationParser`.
- **New source index (menus, etc.)** — the engine is already generic; point it at the new
  `prefix + org` indices once `elastic-sync-service` writes them. No builder changes.
