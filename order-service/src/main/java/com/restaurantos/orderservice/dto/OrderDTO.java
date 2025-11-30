package com.restaurantos.orderservice.dto;

import com.restaurantos.orderservice.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private String orderNumber;

    private String restaurantCode;

    private OrderStatus status;

    private List<OrderItemDTO> orderItems;

    private BigDecimal subtotal;

    private BigDecimal tax;

    private BigDecimal discount;

    private BigDecimal deliveryFee;

    private BigDecimal total;

    private String currency;

    private LocalDateTime createDttm;

    private LocalDateTime updateDttm;

}
