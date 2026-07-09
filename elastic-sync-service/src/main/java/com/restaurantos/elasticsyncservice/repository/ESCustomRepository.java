package com.restaurantos.elasticsyncservice.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import com.restaurantos.elasticsyncservice.dto.IndexDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ESCustomRepository {

    private final ElasticsearchClient client;

    /** Indexes each document's opaque body under its id at the index resolved by {@code indexOf}. */
    public void bulkSave(List<IndexDocument> documents, Function<IndexDocument, String> indexOf) throws IOException {
        if (documents.isEmpty()) return;

        BulkRequest.Builder builder = new BulkRequest.Builder();
        for (IndexDocument document : documents) {
            builder.operations(op -> op.index(idx -> idx
                    .index(indexOf.apply(document))
                    .id(document.getId())
                    .document(document.getBody())));
        }

        BulkResponse response = client.bulk(builder.build());
        logErrors(response);
    }

    /**
     * Deletes documents by _id across every tenant index matched by {@code indexPattern}. The CDC event
     * carries only the id, so we remove by id over the wildcard (ids are globally unique per source).
     */
    public void bulkDelete(Collection<String> ids, String indexPattern) throws IOException {
        if (ids.isEmpty()) return;

        List<String> idList = new ArrayList<>(ids);
        DeleteByQueryResponse response = client.deleteByQuery(d -> d
                .index(indexPattern)
                .refresh(true)
                .query(q -> q.ids(i -> i.values(idList))));

        if (response.failures() != null && !response.failures().isEmpty()) {
            log.error("ES delete-by-query on {} had {} failure(s)", indexPattern, response.failures().size());
        }
        log.debug("Deleted {} doc(s) via delete-by-query on {}", response.deleted(), indexPattern);
    }

    private void logErrors(BulkResponse response) {
        if (response.errors()) {
            String errors = response.items().stream()
                    .filter(item -> item.error() != null)
                    .map(item -> "index=" + item.index() + " id=" + item.id() + ": " + item.error().reason())
                    .collect(Collectors.joining("; "));
            log.error("ES bulk upsert had partial errors: {}", errors);
        }
    }
}
