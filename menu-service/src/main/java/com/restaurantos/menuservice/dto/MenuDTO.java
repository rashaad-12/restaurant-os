package com.restaurantos.menuservice.dto;

import com.restaurantos.menuservice.enums.MenuStatus;
import com.restaurantos.menuservice.model.MenuItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

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

    private MenuStatus status;

    private List<MenuItemDTO> items;

    private LocalDateTime publishDttm;

    private LocalDateTime createDttm;

    private LocalDateTime updateDttm;

}
