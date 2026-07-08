package com.restaurantos.analyticservice.query;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import com.restaurantos.analyticservice.dto.request.AggregationCriteria;
import com.restaurantos.analyticservice.dto.response.Bucket;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

@Component
public class AggregationParser {

    public Map<String, Object> parse(Map<String, Aggregate> aggregates, List<AggregationCriteria> specs) {
        Map<String, Object> output = new LinkedHashMap<>();
        if (isNull(specs)) return output;

        for (AggregationCriteria spec : specs) {
            Aggregate aggregate = aggregates.get(spec.getName());
            if (aggregate != null) {
                output.put(spec.getName(), parseAggregate(aggregate, spec));
            }
        }
        return output;
    }

    private Object parseAggregate(Aggregate aggregate, AggregationCriteria spec) {
        if (aggregate.isSum()) return aggregate.sum().value();

        if (aggregate.isAvg()) return aggregate.avg().value();

        if (aggregate.isMin()) return aggregate.min().value();

        if (aggregate.isMax()) return aggregate.max().value();

        if (aggregate.isValueCount()) return aggregate.valueCount().value();

        if (aggregate.isCardinality()) return aggregate.cardinality().value();

        if (aggregate.isSterms()) {
            return aggregate.sterms().buckets().array().stream()
                    .map(b -> bucket(b.key().stringValue(), b.docCount(), b.aggregations(), spec)).toList();
        }

        if (aggregate.isLterms()) {
            return aggregate.lterms().buckets().array().stream()
                    .map(b -> bucket(b.key(), b.docCount(), b.aggregations(), spec)).toList();
        }

        if (aggregate.isDterms()) {
            return aggregate.dterms().buckets().array().stream()
                    .map(b -> bucket(b.key(), b.docCount(), b.aggregations(), spec)).toList();
        }

        if (aggregate.isDateHistogram()) {
            return aggregate.dateHistogram().buckets().array().stream()
                    .map(b -> bucket(b.keyAsString(), b.docCount(), b.aggregations(), spec)).toList();
        }

        return null;
    }

    private Bucket bucket(Object key, long docCount, Map<String, Aggregate> subAggregates, AggregationCriteria spec) {
        return Bucket.builder()
                .key(key)
                .docCount(docCount)
                .aggregations(parse(subAggregates, spec.getAggregations()))
                .build();
    }
}
