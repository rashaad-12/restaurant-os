package com.restaurantos.menuservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemVariantDTO {

    private String name;

    private BigDecimal priceDelta;

    private boolean available;

}
