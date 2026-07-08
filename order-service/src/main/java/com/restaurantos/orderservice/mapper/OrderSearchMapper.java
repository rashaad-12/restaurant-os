package com.restaurantos.orderservice.mapper;

import com.restaurantos.orderservice.dto.search.OrderDocument;
import com.restaurantos.orderservice.dto.search.OrderItemDocument;
import com.restaurantos.orderservice.dto.search.SearchDocument;
import com.restaurantos.orderservice.model.Order;
import com.restaurantos.orderservice.model.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/** Builds the order search projection ({@link SearchDocument} + {@link OrderDocument}) from the entity. */
@Component
public class OrderSearchMapper {

    public SearchDocument toSearchDocument(Order order) {
        return SearchDocument.builder()
                .id(String.valueOf(order.getId()))
                .routingKey(order.getRestaurantCode())
                .body(toDocument(order))
                .build();
    }

    private OrderDocument toDocument(Order order) {
        List<OrderItem> items = isNull(order.getOrderItems()) ? List.of() : order.getOrderItems();

        return OrderDocument.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .restaurantCode(order.getRestaurantCode())
                .customerId(order.getCustomerId())
                .status(isNull(order.getStatus()) ? null : order.getStatus().name())
                .subtotal(order.getSubtotal())
                .tax(order.getTax())
                .discount(order.getDiscount())
                .deliveryFee(order.getDeliveryFee())
                .total(order.getTotal())
                .currency(order.getCurrency())
                .itemCount(items.stream().mapToInt(item -> isNull(item.getQuantity()) ? 0 : item.getQuantity()).sum())
                .itemNames(items.stream().map(OrderItem::getMenuName).filter(Objects::nonNull).collect(Collectors.joining(", ")))
                .items(items.stream().map(this::toItemDocument).toList())
                .createDttm(order.getCreateDttm())
                .updateDttm(order.getUpdateDttm())
                .build();
    }

    private OrderItemDocument toItemDocument(OrderItem item) {
        return OrderItemDocument.builder()
                .menuCode(item.getMenuCode())
                .menuName(item.getMenuName())
                .menuDescription(item.getMenuDescription())
                .status(isNull(item.getStatus()) ? null : item.getStatus().name())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .build();
    }
}
