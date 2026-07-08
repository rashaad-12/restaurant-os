package com.restaurantos.analyticservice.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SortCriteria {
    private String field;
    private String order = "asc";
}
