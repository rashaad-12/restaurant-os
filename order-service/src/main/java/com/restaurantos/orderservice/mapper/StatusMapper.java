package com.restaurantos.orderservice.mapper;

import com.restaurantos.orderservice.enums.OrderStatus;
import org.springframework.stereotype.Component;

import static java.util.Objects.nonNull;

@Component
public class StatusMapper {

    public OrderStatus toStatusOrDefault(OrderStatus status) {
        return nonNull(status) ? status : OrderStatus.PLACED;
    }

}
