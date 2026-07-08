package com.restaurantos.orderservice.dto.search;

import lombok.Builder;
import lombok.Getter;

/**
 * Generic envelope the search platform consumes: an id (ES {@code _id}), a routing key (tenant/org,
 * used by the platform to resolve the target index), and an opaque {@code body} document. The
 * platform never inspects {@code body} — order-service owns its shape.
 */
@Getter
@Builder
public class SearchDocument {

    private final String id;
    private final String routingKey;
    private final Object body;
}
