package com.restaurantos.analyticservice.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.restaurantos.analyticservice.dto.request.SearchCriteria;
import com.restaurantos.analyticservice.dto.response.AggregationResult;
import com.restaurantos.analyticservice.dto.response.SearchResult;
import com.restaurantos.analyticservice.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/orders")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/{orgCode}/search")
    public SearchResult<ObjectNode> search(@PathVariable String orgCode, @RequestBody SearchCriteria request) {
        return analyticsService.search(orgCode, request);
    }

    @PostMapping("/{orgCode}/aggregate")
    public AggregationResult aggregate(@PathVariable String orgCode, @RequestBody(required = false) SearchCriteria criteria) {
        return analyticsService.aggregate(orgCode, criteria);
    }
}
