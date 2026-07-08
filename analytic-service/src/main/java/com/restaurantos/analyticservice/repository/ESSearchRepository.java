package com.restaurantos.analyticservice.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.io.IOException;

/**
 * Thin Elasticsearch boundary — executes pre-built requests only. Request assembly (query, from/size,
 * sort, aggregations, limits) and response mapping live in the service/query layer, not here.
 */
@Repository
@RequiredArgsConstructor
public class ESSearchRepository {

    private final ElasticsearchClient client;

    public <T> SearchResponse<T> search(SearchRequest request, Class<T> type) throws IOException {
        return client.search(request, type);
    }
}
