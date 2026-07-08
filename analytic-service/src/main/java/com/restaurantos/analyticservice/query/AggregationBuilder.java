package com.restaurantos.analyticservice.query;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import com.restaurantos.analyticservice.config.QueryLimits;
import com.restaurantos.analyticservice.dto.request.AggregationCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/** Builds Elasticsearch aggregations from a generic {@link AggregationCriteria} tree (recursive for sub-aggs). */
@Component
@RequiredArgsConstructor
public class AggregationBuilder {

    private final QueryLimits limits;

    public Map<String, Aggregation> build(List<AggregationCriteria> specs) {
        Map<String, Aggregation> aggregations = new LinkedHashMap<>();
        if (isNull(specs)) return aggregations;

        for (AggregationCriteria spec : specs) {
            if (isNull(spec.getName()) || isNull(spec.getType())) continue;
            aggregations.put(spec.getName(), buildOne(spec));
        }

        return aggregations;
    }

    private Aggregation buildOne(AggregationCriteria spec) {
        String field = spec.getField();
        Aggregation.Builder builder = new Aggregation.Builder();

        switch (spec.getType()) {
            case SUM -> builder.sum(a -> a.field(field));
            case AVG -> builder.avg(a -> a.field(field));
            case MIN -> builder.min(a -> a.field(field));
            case MAX -> builder.max(a -> a.field(field));
            case VALUE_COUNT -> builder.valueCount(a -> a.field(field));
            case CARDINALITY -> builder.cardinality(a -> a.field(field));
            case TERMS -> {
                int termsSize = Math.min(isNull(spec.getBucketSize()) ? 10 : spec.getBucketSize(), limits.getMaxTermsSize());
                builder.terms(a -> a.field(field).size(termsSize));
            }
            case DATE_HISTOGRAM -> builder.dateHistogram(a -> a.field(field).calendarInterval(interval(spec.getInterval())));
        }

        if (nonNull(spec.getAggregations())) {
            for (AggregationCriteria sub : spec.getAggregations()) {
                if (nonNull(sub.getName()) && nonNull(sub.getType())) {
                    builder.aggregations(sub.getName(), buildOne(sub));
                }
            }
        }

        return builder.build();
    }

    private CalendarInterval interval(String interval) {
        if (isNull(interval)) return CalendarInterval.Day;

        return switch (interval.toLowerCase()) {
            case "minute" -> CalendarInterval.Minute;
            case "hour" -> CalendarInterval.Hour;
            case "week" -> CalendarInterval.Week;
            case "month" -> CalendarInterval.Month;
            case "quarter" -> CalendarInterval.Quarter;
            case "year" -> CalendarInterval.Year;
            default -> CalendarInterval.Day;
        };
    }
}
