package com.restaurantos.analyticservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class AggregationResult {
    private long total;
    private Map<String, Object> aggregations;
}
