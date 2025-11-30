package com.restaurantos.menuservice.model;

import com.restaurantos.menuservice.enums.MenuItemType;
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
public class MenuItem {

    private String code;

    private String name;

    private String description;

    private BigDecimal price;

    private Boolean available;

    private String category;

    private List<String> tags;

    private MenuItemType type;

    private List<MenuItemVariant> variants;

    private List<String> comboItems;

}
