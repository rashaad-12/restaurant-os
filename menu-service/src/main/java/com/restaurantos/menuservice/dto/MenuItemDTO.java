package com.restaurantos.menuservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemDTO {

    private String name;

    private String description;

    private BigDecimal price;

    private boolean available;

    private String category;

    private List<String> tags;

    private String type;

    private List<MenuItemVariantDTO> variants;

    private List<String> comboItems;

}
