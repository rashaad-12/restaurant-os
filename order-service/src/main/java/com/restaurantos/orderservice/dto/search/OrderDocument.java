package com.restaurantos.orderservice.dto.search;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The denormalized order search document. order-service <em>owns</em> this projection; the platform
 * (elastic-sync-service) indexes it opaquely and analytic-service queries it. Changing this shape is a
 * change to the search index contract — coordinate with the index template.
 */
@Getter
@Builder
public class OrderDocument {

    private final Long id;
    private final String orderNumber;
    private final String restaurantCode;
    private final String customerId;
    private final String status;

    private final BigDecimal subtotal;
    private final BigDecimal tax;
    private final BigDecimal discount;
    private final BigDecimal deliveryFee;
    private final BigDecimal total;
    private final String currency;

    private final Integer itemCount;
    private final String itemNames;
    private final List<OrderItemDocument> items;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final LocalDateTime createDttm;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final LocalDateTime updateDttm;
}
