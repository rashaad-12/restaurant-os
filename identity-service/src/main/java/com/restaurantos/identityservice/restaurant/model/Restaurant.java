package com.restaurantos.identityservice.restaurant.model;

import com.restaurantos.identityservice.user.enums.EntityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "restaurants")
public class Restaurant {

    @Id
    private String id;

    private String restaurantCode;

    private String name;

    private String type;

    private String parentOrgCode;

    private EntityStatus status;

    private String createdBy;

    @CreatedDate
    private LocalDateTime createDttm;

    @LastModifiedDate
    private LocalDateTime updateDttm;

}

