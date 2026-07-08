# elastic-service — Architecture

The platform's **change-data-capture → Elasticsearch sync worker** (Spring `application.name:
elastic-sync-service`). It keeps per-tenant search indices in step with the source-of-truth
databases by consuming CDC events, enriching them from the owning service, and bulk-writing them to
Elasticsearch. It is a **headless worker** — no domain HTTP API, only actuator — and is deliberately
**generic**: it knows nothing about the order domain. Every domain-specific fact (which topic, which
enrichment endpoint, which index prefix) is configuration, so a new source is added without code
changes.

Runnable Spring Boot application (`bootJar`). Local dev: port **8185**. Not a `core-security`
verifier (it exposes no protected API); instead it is a **SYSTEM client** of identity-service and a
Kafka consumer.

Depends on: `spring-kafka`, the Elasticsearch Java client (`co.elastic.clients:elasticsearch-java`),
Jackson, and — over the wire — **identity-service** (for a SYSTEM token) and each **source service**
(for enrichment).

---

## 1. Where it sits — the CDC pipeline

```
  order-service ──writes──► MySQL order_db  (binlog: ROW + GTID, enabled in docker-compose)
                                   │
                          Debezium (kafka-connect)          captures row changes
                                   │
                                   ▼
                    Kafka topic  dev.restaurant.orders       (one topic per source)
                                   │
                                   ▼
   ┌─────────────────────────── elastic-service ───────────────────────────┐
   │ ESSyncConsumer      batch KafkaListener (manual ack)                     │
   │ ChangeEventParser   Debezium JSON → ChangeEvent; tombstones skipped;     │
   │                     unparseable → DLQ                                    │
   │ SyncServiceImpl     group by topic → collect upsert/delete ids           │
   │ EnrichmentClient ───POST ids + SYSTEM JWT──► order-service              │
   │                     /order-api/v1/order/search-documents/by-ids          │
   │                     ◄── List<IndexDocument> {id, routingKey, body}       │
   │ ESCustomRepository  bulk index (per-tenant) + delete-by-query            │
   └──────────────────────────────┬──────────────────────────────────────────┘
                                   ▼
                Elasticsearch   dev_orders_<org>   (one index per tenant)
```

**Why enrichment instead of indexing the CDC row directly.** A Debezium row is flat and
normalized (one `orders` row, no items, DB-internal shape). The searchable document is a denormalized
aggregate the *owning* service composes (order + items + derived fields). So the CDC event is used
only as a **change signal + id**; the actual document is pulled back from order-service, which owns
its shape. This keeps the search document's schema owned by the domain service, not smeared across a
sync worker. (See [`memory: elastic-enrichment-architecture`] and order-service `getSearchDocuments`.)

---

## 2. Component map

```
consumer/ESSyncConsumer        batch @KafkaListener; delegates to SyncService; manual ack; rethrow → retry
service/SyncService(Impl)      per-topic: collect upsert/delete id sets, enrich, bulk-write
util/ChangeEventParser         Debezium JSON → ChangeEvent; skip tombstones; DLQ on parse failure
client/EnrichmentClient        generic POST(ids)→List<IndexDocument>, Bearer SYSTEM token
client/SystemTokenProvider     fetches + caches the SYSTEM JWT from identity-service (double-checked, skew-refreshed)
repository/ESCustomRepository  Elasticsearch bulk index + delete-by-query over a wildcard
service/IndexService(Impl)     thin wrapper over the repo (bulkSave/bulkDelete + logging)
producer/DlqPublisher          idempotent producer → DLQ topic for poison records
config/
  KafkaConsumerConfig          batch factory, MANUAL ack, DefaultErrorHandler (retry+backoff→DLQ)
  SourceProperties             sync.sources.* — topic → enrichment endpoint → index prefix
  IndexResolver                (prefix, routingKey) → tenant index name; wildcard for deletes
  RestClientConfig             RestClients for identity-service + enrichment (timeouts)
  ElasticSearchConfig          ElasticsearchClient (Rest5 transport, basic auth, timeouts)
  SourceProperties/…           binding for integration.* and elasticsearch.*
dto/  ChangeEvent{before,after,op} · ChangeRecord{id} · IndexDocument{id,routingKey,body} · ServiceToken
enums/ChangeEventOperation     CREATE | UPDATE | DELETE (Debezium op)
```

---

## 3. Processing semantics (the important details)

- **Batch consume, manual ack.** The listener is a **batch** listener with `AckMode.MANUAL` and
  `enable.auto.commit=false`. Offsets commit **only after** the whole batch is processed and indexed;
  any thrown exception skips the ack, so the batch is redelivered — **at-least-once** delivery.
- **Idempotent writes.** Upserts index by the document's own `id`, deletes are by `_id`; replaying a
  batch converges to the same index state. At-least-once + idempotent writes = effectively-once
  outcome without distributed transactions.
- **Upsert/delete coalescing.** Within a topic's batch, ids are collapsed into two sets: a later
  DELETE removes an id from the upsert set and vice-versa, so each id is actioned once with its final
  intent.
- **Late-delete detection.** Ids requested for upsert but **not returned** by enrichment were deleted
  between the CDC event and the lookup; they are moved into the delete set — the index never keeps a
  ghost row.
- **Tenant fan-out.** `routingKey` on each `IndexDocument` selects the tenant index
  (`prefix + org`, lower-cased); deletes run as `delete-by-query` over the `prefix + "*"` wildcard
  because the CDC delete event carries only the id, not the tenant.
- **Error handling / DLQ.** `DefaultErrorHandler` retries with exponential backoff + jitter (1s→30s,
  5 attempts); parse failures (`JsonParseException`/`JsonMappingException`) are **non-retryable** and
  go straight to the DLQ; anything still failing after retries is published to the DLQ by
  `DlqPublisher` (idempotent producer, `acks=all`). Poison messages never block the partition.
- **SYSTEM auth.** `SystemTokenProvider` exchanges `client-id`/`client-secret` at
  `/auth-api/v1/auth/system/token` for a short-lived SYSTEM JWT, caches it, and refreshes 60s before
  expiry under a double-checked lock. `EnrichmentClient` sends it as `Bearer`; order-service's
  enrichment endpoint is gated `hasRole('SYSTEM')`.

---

## 4. Configuration — sources are data, not code

Adding a new source (any future service that needs a search index) is **config only**:

```yaml
sync:
  sources:
    orders:                                   # logical name
      topic: dev.restaurant.orders            # CDC topic to consume
      index-prefix: dev_orders_               # tenant index prefix → dev_orders_<org>
      enrichment:
        base-url: http://localhost:8184       # the owning service
        path: /order-api/v1/order/search-documents/by-ids
```

`SourceProperties.topics()` feeds the `@KafkaListener` topic list via SpEL
(`#{@sourceProperties.topics()}`), and `forTopic(...)` routes each consumed batch back to its source.
Other knobs: `spring.kafka.consumer.*` (bootstrap, group), `kafka.consumer.concurrency`,
`kafka.topics.dlq`, `elasticsearch.*` (host/auth/timeouts), `integration.identity-service.*`
(client-id/secret, base-url), `integration.http.*` (connect/read timeouts).

---

## 5. Invariants — what must not change lightly

1. **The worker stays domain-agnostic.** No order-specific types leak in. Domain knowledge lives in
   `sync.sources.*` and in the enrichment payload (`IndexDocument.body` is opaque). Adding a source
   must remain config-only.
2. **At-least-once + idempotent writes.** Never ack before a batch is durably indexed, and never make
   a write non-idempotent (always index/delete by the document's own id). These two together are the
   correctness contract.
3. **Enrichment is the source of truth for document shape.** elastic-service must not synthesize a
   search document from the raw CDC row; it always fetches from the owning service.
4. **SYSTEM credential handling.** The client-secret is a secret (env/secrets manager in prod, never
   committed); the SYSTEM token is short-lived and cached, never logged.
5. **Poison records go to the DLQ, never block the partition.** Parse/permanent failures must remain
   non-retryable and DLQ-routed.

---

## 6. Rating — 8 / 10

**Reviewed:** 2026-07-08 (source).

| Dimension | Score | Notes |
|---|---|---|
| Architecture & correctness | 9 / 10 | Textbook CDC-enrichment-index pipeline: manual-ack at-least-once, idempotent writes, upsert/delete coalescing, late-delete detection, DLQ with backoff+jitter. |
| Genericity / extensibility | 9 / 10 | Fully source-driven via `sync.sources.*`; a new source is pure config. `ChangeEventParser` and `EnrichmentClient` carry no domain. |
| Resilience | 8 / 10 | Retry/backoff/jitter, non-retryable parse errors, idempotent DLQ producer, token refresh with skew. |
| Security posture | 7 / 10 | Correct SYSTEM-token client with skew refresh; loses points only for committed local secrets and no TLS to ES/Kafka in dev config. |
| Observability | 5 / 10 | Good structured debug logs + actuator health, but no metrics on lag/throughput/DLQ rate and no tracing. |
| Testing | 3 / 10 | `spring-kafka-test` is on the classpath but there are no tests yet for the coalescing/late-delete logic — the highest-value place to add them. |

**Verdict:** the most operationally sophisticated service in the repo. The design is production-grade;
the gaps are test coverage and metrics, not the pipeline itself.

### Known gaps
| Sev | Finding |
|---|---|
| 🟠 | No tests for `SyncServiceImpl` coalescing / late-delete / `ChangeEventParser` DLQ routing (the subtle, high-risk logic). |
| 🟠 | Committed local secrets (`elasticsearch.password`, `client-secret`) and plaintext ES/Kafka in dev config. |
| 🟡 | No consumer-lag / DLQ-rate / enrichment-latency metrics. |
| 🟡 | `delete-by-query` with `refresh=true` on every batch is heavy under high delete volume. |
| 🟡 | Enrichment call has no explicit retry/circuit-breaker (relies on the whole batch retrying), so a slow source amplifies redelivery. |

---

## 7. Next plans (roadmap)

**P0 — Test the correctness-critical logic**
1. Unit-test `SyncServiceImpl`: upsert/delete coalescing, late-delete promotion, multi-topic batches.
2. Unit-test `ChangeEventParser`: tombstone skip, missing-`op` rejection, DLQ routing on bad JSON.
3. Integration test with `spring-kafka-test` (embedded broker) + a stubbed enrichment endpoint and a
   Testcontainers Elasticsearch, asserting end-to-end index/delete convergence.

**P1 — Operability**
4. Micrometer metrics: consumer lag, records/sec, enrichment latency, bulk-error rate, DLQ count;
   Prometheus scrape + alert on DLQ growth and lag.
5. Distributed tracing (OTel) propagated from the CDC event through enrichment into the ES write.
6. A DLQ replay tool/endpoint (drain the DLQ back into the source topic after a fix).

**P2 — Robustness & scale**
7. Dedicated retry/circuit-breaker (Resilience4j) around `EnrichmentClient` so a degraded source
   doesn't turn into unbounded batch redelivery.
8. Index lifecycle: templates/ILM for the `dev_orders_*` indices (mappings, rollover, retention) —
   today indices are created implicitly by first write.
9. Generalise beyond orders: add menu/user sources (emit CDC + an enrichment endpoint on those
   services) purely via `sync.sources.*`.

---

## 8. Production optimisation plan

**Throughput & batching**
- Tune `max.poll.records`, `fetch.min.bytes`/`fetch.max.wait.ms`, and `kafka.consumer.concurrency`
  against partition count (concurrency ≤ partitions). Batch enrichment already amortises the network
  round-trip — keep enrichment page sizes aligned with poll size.
- Prefer bulk index + a single `delete-by-query` per batch (already done); avoid `refresh=true` on
  the hot path — let ES refresh on its interval and reserve forced refresh for correctness-critical
  deletes only.

**Reliability**
- Size retry attempts/backoff to the source's real recovery time; ensure the DLQ has its own
  monitoring and replay path so poison messages are visible, not silently dropped.
- Idempotent producer (`enable.idempotence=true`, `acks=all`) is already set for the DLQ — keep it.

**Resource & connection tuning**
- Elasticsearch client: explicit connect/socket timeouts (present), a bounded connection pool, and
  retry on 429/503; back-pressure on bulk rejections.
- Cache and reuse the SYSTEM token (present); ensure clock-skew refresh (60s) is comfortably inside
  the token TTL.

**Security & config hardening**
- Externalise `client-secret` and ES credentials to env/secrets; enable TLS + auth to Elasticsearch
  and (where applicable) Kafka in production.
- Run one consumer group across N replicas for horizontal scale; partitions bound parallelism.

**Observability**
- Ship the P1 metrics + tracing before scaling load; alert on lag, DLQ rate, and enrichment error
  rate. Structured JSON logs already include topic/partition/offset and id counts — keep that.
