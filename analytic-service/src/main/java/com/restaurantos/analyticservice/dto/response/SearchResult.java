package com.restaurantos.analyticservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class SearchResult<T> {
    private long total;
    private int page;
    private int size;
    private List<T> items;
}
