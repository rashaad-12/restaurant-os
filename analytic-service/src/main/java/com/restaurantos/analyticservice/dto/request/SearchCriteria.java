package com.restaurantos.analyticservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class SearchCriteria {
    private int page = 0;
    private int size = 20;
    private Condition filter;
    private List<SortCriteria> sort;
    private List<AggregationCriteria> aggregations;
}
