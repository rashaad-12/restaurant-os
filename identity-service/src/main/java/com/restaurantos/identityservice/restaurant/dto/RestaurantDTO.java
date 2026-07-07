package com.restaurantos.identityservice.restaurant.dto;

import com.restaurantos.identityservice.user.enums.EntityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDTO {

    private String restaurantCode;

    private String name;

    private String type;

    private String parentOrgCode;

    private EntityStatus status;

    private String createdBy;

    private LocalDateTime createDttm;

    private LocalDateTime updateDttm;

}
