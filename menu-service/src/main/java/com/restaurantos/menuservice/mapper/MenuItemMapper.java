package com.restaurantos.menuservice.mapper;

import com.restaurantos.menuservice.dto.MenuItemDTO;
import com.restaurantos.menuservice.model.MenuItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = { MenuItemVariantMapper.class },
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface MenuItemMapper {

    MenuItem toEntity(MenuItemDTO dto);

    List<MenuItem> toEntityList(List<MenuItemDTO> dtos);

    @Mapping(target = "markedForDeletion", ignore = true)
    MenuItemDTO toDTO(MenuItem item);

    List<MenuItemDTO> toDTOList(List<MenuItem> items);

    void updateEntityFromDTO(MenuItemDTO dto, @MappingTarget MenuItem entity);

}