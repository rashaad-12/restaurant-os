package com.restaurantos.analyticservice.service.impl;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.restaurantos.analyticservice.config.IndexResolver;
import com.restaurantos.analyticservice.dto.request.AggregationCriteria;
import com.restaurantos.analyticservice.dto.request.SearchCriteria;
import com.restaurantos.analyticservice.dto.response.AggregationResult;
import com.restaurantos.analyticservice.dto.response.SearchResult;
import com.restaurantos.analyticservice.query.AggregationParser;
import com.restaurantos.analyticservice.query.FieldTypes;
import com.restaurantos.analyticservice.query.MappingFieldRegistry;
import com.restaurantos.analyticservice.query.SearchRequestBuilder;
import com.restaurantos.analyticservice.repository.ESSearchRepository;
import com.restaurantos.analyticservice.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final IndexResolver indexResolver;
    private final MappingFieldRegistry fieldRegistry;
    private final SearchRequestBuilder requestBuilder;
    private final ESSearchRepository repository;
    private final AggregationParser aggregationParser;

    @Override
    public SearchResult<ObjectNode> search(String orgId, SearchCriteria criteria) {
        try {
            String index = indexResolver.forOrg(orgId);
            FieldTypes types = fieldRegistry.forIndex(index);
            SearchRequest request = requestBuilder.forSearch(index, criteria, types);
            SearchResponse<ObjectNode> response = repository.search(request, ObjectNode.class);
            return toSearchResult(response, request);
        } catch (IOException e) {
            throw new RuntimeException("Search failed", e);
        }
    }

    @Override
    public AggregationResult aggregate(String orgId, SearchCriteria criteria) {
        try {
            String index = indexResolver.forOrg(orgId);
            FieldTypes types = fieldRegistry.forIndex(index);
            SearchRequest esRequest = requestBuilder.forAggregate(index, criteria, types);
            SearchResponse<Void> response = repository.search(esRequest, Void.class);

            List<AggregationCriteria> specs = criteria == null ? List.of() : criteria.getAggregations();
            long total = response.hits().total() == null ? 0 : response.hits().total().value();
            return AggregationResult.builder()
                    .total(total)
                    .aggregations(aggregationParser.parse(response.aggregations(), specs))
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Aggregation failed", e);
        }
    }

    private SearchResult<ObjectNode> toSearchResult(SearchResponse<ObjectNode> response, SearchRequest request) {
        List<ObjectNode> items = new ArrayList<>();
        for (Hit<ObjectNode> hit : response.hits().hits()) {
            ObjectNode doc = hit.source();
            if (doc != null) {
                if (!doc.has("id")) doc.put("id", hit.id());
                items.add(doc);
            }
        }

        int size = request.size() == null ? 0 : request.size();
        int from = request.from() == null ? 0 : request.from();
        int page = size > 0 ? from / size : 0;
        long total = response.hits().total() == null ? items.size() : response.hits().total().value();

        return SearchResult.<ObjectNode>builder()
                .total(total).page(page).size(size).items(items).build();
    }
}
