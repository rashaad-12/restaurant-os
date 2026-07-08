package com.restaurantos.analyticservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class Bucket {
    private Object key;
    private long docCount;
    private Map<String, Object> aggregations;
}
