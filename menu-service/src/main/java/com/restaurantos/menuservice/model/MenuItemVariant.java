package com.restaurantos.menuservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemVariant {

    private String name;

    private BigDecimal priceDelta;

    private Boolean available;
}
