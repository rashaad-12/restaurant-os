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

## 6. Extensibility Rules — Dos & Don'ts

**Do**
- **Do** add a new source purely via `sync.sources.*` plus an enrichment endpoint on the owning
  service — no code in this worker.
- **Do** keep `ChangeEventParser` and `EnrichmentClient` free of any domain type.
- **Do** treat `IndexDocument.body` as opaque JSON owned by the source service.
- **Do** keep every write idempotent (index/delete by the document's own id) and ack only after a
  durable index.

**Don't**
- **Don't** synthesize a search document from the raw CDC row — always enrich from the owner.
- **Don't** ack a batch before it is indexed, and don't introduce a non-idempotent write.
- **Don't** make parse/permanent failures retryable — they must go straight to the DLQ so they can't
  block the partition.
- **Don't** log the SYSTEM token or commit the client-secret.

---

## 7. How to extend

- **Onboard a new domain to search** — add a `sync.sources.<name>` block (topic, index-prefix,
  enrichment base-url + path) and expose a SYSTEM-gated `by-ids` enrichment endpoint on the owning
  service that returns `List<IndexDocument>`. No worker code changes.
- **New CDC source shape** — if a source's change event isn't Debezium-shaped, extend
  `ChangeEventParser`; keep the output `ChangeEvent` domain-neutral.
