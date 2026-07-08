package com.restaurantos.analyticservice.query;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.restaurantos.analyticservice.config.QueryLimits;
import com.restaurantos.analyticservice.dto.request.AggregationCriteria;
import com.restaurantos.analyticservice.dto.request.SearchCriteria;
import com.restaurantos.analyticservice.dto.request.SortSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Assembles Elasticsearch {@link SearchRequest}s from the API request DTOs — query, sort, pagination,
 * aggregations — and enforces the query guardrails. Kept out of the repository so the repo does nothing
 * but execute.
 */
@Component
@RequiredArgsConstructor
public class SearchRequestBuilder {

    private final QueryBuilder queryBuilder;
    private final AggregationBuilder aggregationBuilder;
    private final QueryLimits limits;

    public SearchRequest forSearch(String index, SearchCriteria criteria, FieldTypes types) {
        int page = Math.max(criteria.getPage(), 0);
        int size = Math.min(criteria.getSize() <= 0 ? 20 : criteria.getSize(), limits.getMaxPageSize());
        int from = page * size;
        if ((long) from + size > limits.getMaxResultWindow()) {
            throw new IllegalArgumentException("Pagination window (" + (from + size)
                    + ") exceeds max " + limits.getMaxResultWindow() + "; narrow the query instead of deep-paging");
        }

        Query query = queryBuilder.build(types, criteria.getFilter());
        List<SortOptions> sort = buildSort(criteria.getSort());

        return SearchRequest.of(s -> s
                .index(index)
                .ignoreUnavailable(true)
                .allowNoIndices(true)
                .timeout(limits.getQueryTimeoutMs() + "ms")
                .query(query)
                .sort(sort)
                .from(from)
                .size(size)
                .trackTotalHits(t -> t.enabled(true)));
    }

    public SearchRequest forAggregate(String index, SearchCriteria criteria, FieldTypes types) {
        List<AggregationCriteria> specs = criteria == null ? List.of() : criteria.getAggregations();
        validateAggregations(specs);

        Query query = queryBuilder.build(types, criteria == null ? null : criteria.getFilter());
        Map<String, Aggregation> aggregations = aggregationBuilder.build(specs);

        return SearchRequest.of(s -> s
                .index(index)
                .ignoreUnavailable(true)
                .allowNoIndices(true)
                .timeout(limits.getQueryTimeoutMs() + "ms")
                .size(0)
                .query(query)
                .trackTotalHits(t -> t.enabled(true))
                .aggregations(aggregations));
    }

    private List<SortOptions> buildSort(List<SortSpec> specs) {
        // No domain-specific default sort — an unknown field would error on other indices. Callers
        // that want ordering pass an explicit sort; otherwise ES applies its default (_score/_doc).
        List<SortOptions> options = new ArrayList<>();
        if (specs == null || specs.isEmpty()) {
            return options;
        }
        for (SortSpec spec : specs) {
            if (spec.getField() == null) continue;
            SortOrder order = "desc".equalsIgnoreCase(spec.getOrder()) ? SortOrder.Desc : SortOrder.Asc;
            options.add(SortOptions.of(so -> so.field(fs -> fs.field(spec.getField()).order(order))));
        }
        return options;
    }

    private void validateAggregations(List<AggregationCriteria> specs) {
        int count = countAggregations(specs);
        if (count > limits.getMaxAggregations()) {
            throw new IllegalArgumentException("Too many aggregations: " + count + " (max " + limits.getMaxAggregations() + ")");
        }
        int depth = aggregationDepth(specs);
        if (depth > limits.getMaxAggregationDepth()) {
            throw new IllegalArgumentException("Aggregation nesting too deep: " + depth + " (max " + limits.getMaxAggregationDepth() + ")");
        }
    }

    private int countAggregations(List<AggregationCriteria> specs) {
        if (specs == null) return 0;
        int count = 0;
        for (AggregationCriteria spec : specs) {
            count += 1 + countAggregations(spec.getAggregations());
        }
        return count;
    }

    private int aggregationDepth(List<AggregationCriteria> specs) {
        if (specs == null || specs.isEmpty()) return 0;
        int max = 0;
        for (AggregationCriteria spec : specs) {
            max = Math.max(max, aggregationDepth(spec.getAggregations()));
        }
        return 1 + max;
    }
}
