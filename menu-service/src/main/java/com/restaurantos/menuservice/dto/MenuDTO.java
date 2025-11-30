package com.restaurantos.menuservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.restaurantos.common.enums.DateConstants;
import com.restaurantos.menuservice.enums.MenuStatus;
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

    private String restaurantCode;

    private String code;

    private String name;

    private String description;

    private MenuStatus status;

    private List<MenuItemDTO> items;

    @JsonFormat(pattern = DateConstants.YYYY_MM_DD_HH_MM)
    private LocalDateTime publishDttm;

    @JsonFormat(pattern = DateConstants.YYYY_MM_DD_HH_MM)
    private LocalDateTime createDttm;

    @JsonFormat(pattern = DateConstants.YYYY_MM_DD_HH_MM)
    private LocalDateTime updateDttm;

}
