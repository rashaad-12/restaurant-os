package com.restaurantos.analyticservice.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.restaurantos.analyticservice.dto.request.SearchCriteria;
import com.restaurantos.analyticservice.dto.response.AggregationResult;
import com.restaurantos.analyticservice.dto.response.SearchResult;
import com.restaurantos.analyticservice.service.AnalyticsService;
import com.restaurantos.coresecurity.authz.ScopeGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/analytics/orders")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ScopeGuard scopeGuard;

    @PostMapping("/{orgCode}/search")
    @PreAuthorize("hasAnyRole('SYSTEM', 'ADMIN')")
    public SearchResult<ObjectNode> search(@PathVariable String orgCode, @RequestBody SearchCriteria request) {
        assertScope(orgCode);
        return analyticsService.search(orgCode, request);
    }

    @PostMapping("/{orgCode}/aggregate")
    @PreAuthorize("hasAnyRole('SYSTEM', 'ADMIN')")
    public AggregationResult aggregate(@PathVariable String orgCode, @RequestBody(required = false) SearchCriteria criteria) {
        assertScope(orgCode);
        return analyticsService.aggregate(orgCode, criteria);
    }

    private void assertScope(String orgCode) {
        if (!scopeGuard.hasRole("SYSTEM")) {
            scopeGuard.assertCanView(Set.of(orgCode));
        }
    }
}
