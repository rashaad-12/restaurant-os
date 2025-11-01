package com.restaurantos.menuservice.mapper;

import com.restaurantos.menuservice.dto.MenuDTO;
import com.restaurantos.menuservice.dto.MenuItemDTO;
import com.restaurantos.menuservice.dto.MenuItemVariantDTO;
import com.restaurantos.menuservice.enums.MenuStatus;
import com.restaurantos.menuservice.model.Menu;
import com.restaurantos.menuservice.model.MenuItem;
import com.restaurantos.menuservice.model.MenuItemVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.nonNull;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MenuMapper {

    @Mapping(target = "id", expression = "java(generateId())")
    @Mapping(target = "status", expression = "java(toMenuStatusOrDefault(request.getStatus()))")
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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createDttm", ignore = true)
    @Mapping(target = "publishDttm", ignore = true)
    void updateMenuFromDto(MenuDTO dto, @MappingTarget Menu entity);

    default String generateId() {
        return UUID.randomUUID().toString();
    }

    default MenuStatus toMenuStatusOrDefault(String status) {
        try {
            return nonNull(status) ? MenuStatus.valueOf(status) : MenuStatus.DRAFT;
        } catch (IllegalArgumentException e) {
            return MenuStatus.DRAFT;
        }
    }
}