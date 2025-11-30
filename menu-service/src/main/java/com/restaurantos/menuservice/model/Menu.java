package com.restaurantos.menuservice.model;

import com.restaurantos.menuservice.enums.MenuStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "menu")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "unique_res_menu_code", def = "{'restaurantCode': 1, 'code': 1}", unique = true)
public class Menu {

    @Id
    private String id;

    private String restaurantCode;

    private String code;

    private String name;

    private String description;

    private MenuStatus status;

    private List<MenuItem> items;

    private LocalDateTime publishDttm;

    @CreatedDate
    private LocalDateTime createDttm;

    @LastModifiedDate
    private LocalDateTime updateDttm;

}
