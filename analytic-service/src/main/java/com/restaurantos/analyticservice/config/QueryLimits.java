package com.restaurantos.analyticservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Guardrails for the generic (tenant-facing) query/aggregation API. A fully generic query surface is
 * a DoS foot-gun without bounds — these cap result windows, bucket sizes, aggregation breadth/depth,
 * and per-query time.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "analytics.limits")
public class QueryLimits {

    private int maxPageSize = 100;
    private int maxResultWindow = 10000;
    private int maxTermsSize = 1000;
    private int maxAggregations = 20;
    private int maxAggregationDepth = 3;
    private long queryTimeoutMs = 5000;
}
