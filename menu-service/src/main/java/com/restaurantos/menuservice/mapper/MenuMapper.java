package com.restaurantos.menuservice.mapper;

import com.restaurantos.menuservice.dto.MenuDTO;
import com.restaurantos.menuservice.dto.MenuItemDTO;
import com.restaurantos.menuservice.dto.MenuItemVariantDTO;
import com.restaurantos.menuservice.model.Menu;
import com.restaurantos.menuservice.model.MenuItem;
import com.restaurantos.menuservice.model.MenuItemVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface MenuMapper {

    @Mapping(target = "id", expression = "java(generateId())")
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "publishDttm", ignore = true)
    @Mapping(target = "createDttm", ignore = true)
    @Mapping(target = "updateDttm", ignore = true)
    Menu toMenuEntity(MenuDTO request);

    List<MenuItem> toMenuItemEntityList(List<MenuItemDTO> items);

    @Mapping(target = "id", expression = "java(generateId())")
    MenuItem toMenuItemEntity(MenuItemDTO request);

    @Mapping(target = "id", expression = "java(generateId())")
    MenuItemVariant toMenuItemVariantEntity(MenuItemVariantDTO request);

    List<MenuItemVariant> toMenuItemVariantEntityList(List<MenuItemVariantDTO> variants);

    MenuDTO toMenuDTO(Menu menu);

    List<MenuItemDTO> toMenuItemDTOList(List<MenuItem> items);

    MenuItemDTO toMenuItemDTO(MenuItem item);

    MenuItemVariantDTO toMenuItemVariantDTO(MenuItemVariant variant);

    List<MenuItemVariantDTO> toMenuItemVariantDTOList(List<MenuItemVariant> variants);

    default String generateId() {
        return UUID.randomUUID().toString();
    }
}