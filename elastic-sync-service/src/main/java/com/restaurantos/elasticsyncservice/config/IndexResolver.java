package com.restaurantos.elasticsyncservice.config;

import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Resolves per-tenant index names from a source's prefix and a routing key. Index naming is a
 * platform concern; the prefix comes from per-source config, the routing key from the document.
 */
@Component
public class IndexResolver {

    /** Concrete tenant index for an upsert, e.g. prefix {@code dev_orders_} + org {@code r1}. */
    public String resolve(String prefix, String routingKey) {
        String org = isBlank(routingKey) ? "unknown" : routingKey.trim();
        return (prefix + org).toLowerCase();
    }

    /** Wildcard over all tenant indices for a source; used for deletes (routing key unknown). */
    public String pattern(String prefix) {
        return (prefix + "*").toLowerCase();
    }
}
