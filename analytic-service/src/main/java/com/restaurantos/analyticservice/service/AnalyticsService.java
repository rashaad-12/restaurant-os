package com.restaurantos.analyticservice.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.restaurantos.analyticservice.dto.request.SearchCriteria;
import com.restaurantos.analyticservice.dto.response.AggregationResult;
import com.restaurantos.analyticservice.dto.response.SearchResult;

public interface AnalyticsService {
    SearchResult<ObjectNode> search(String orgId, SearchCriteria criteria);
    AggregationResult aggregate(String orgId, SearchCriteria criteria);
}
