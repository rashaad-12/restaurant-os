package com.restaurantos.menuservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuDTO {

    private String id;

    private String restaurantCode;

    private String name;

    private String description;

    private String status;

    private List<MenuItemDTO> items;

    private LocalDateTime publishDttm;

    private LocalDateTime createDttm;

    private LocalDateTime updateDttm;

}
