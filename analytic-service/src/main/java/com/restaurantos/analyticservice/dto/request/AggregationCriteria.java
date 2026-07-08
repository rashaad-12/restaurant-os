package com.restaurantos.analyticservice.dto.request;

import com.restaurantos.analyticservice.enums.AggregationType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AggregationCriteria {
    private String name;
    private String field;
    private AggregationType type;
    private String interval;
    private Integer bucketSize;
    private List<AggregationCriteria> aggregations;
}
