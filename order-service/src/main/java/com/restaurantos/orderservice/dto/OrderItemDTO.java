package com.restaurantos.orderservice.dto;

import com.restaurantos.orderservice.enums.OrderStatus;
import com.restaurantos.orderservice.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {

    private String menuCode;

    private String menuName;

    private String menuDescription;

    private OrderStatus status;

    private BigDecimal price;

    private Integer quantity;

    private Order order;

    private LocalDateTime createDttm;

    private LocalDateTime updateDttm;

}

