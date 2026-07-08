package com.restaurantos.orderservice.dto.search;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** Nested line item within {@link OrderDocument}. Owned by order-service (it defines its search shape). */
@Getter
@Builder
public class OrderItemDocument {

    private final String menuCode;
    private final String menuName;
    private final String menuDescription;
    private final String status;
    private final BigDecimal price;
    private final Integer quantity;
}
