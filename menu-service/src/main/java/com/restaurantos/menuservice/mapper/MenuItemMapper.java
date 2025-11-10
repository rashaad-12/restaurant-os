package com.restaurantos.menuservice.mapper;

import com.restaurantos.menuservice.dto.MenuItemDTO;
import com.restaurantos.menuservice.model.MenuItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.UUID;

@Mapper(
        componentModel = "spring",
        uses = { MenuItemVariantMapper.class },
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface MenuItemMapper {

    @Mapping(target = "id", expression = "java(generateId())")
    MenuItem toEntity(MenuItemDTO dto);

    List<MenuItem> toEntityList(List<MenuItemDTO> dtos);

    MenuItemDTO toDTO(MenuItem item);

    List<MenuItemDTO> toDTOList(List<MenuItem> items);

    default String generateId() {
        return UUID.randomUUID().toString();
    }

}