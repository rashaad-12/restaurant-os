package com.restaurantos.menuservice.mapper;

import com.restaurantos.menuservice.dto.MenuItemVariantDTO;
import com.restaurantos.menuservice.model.MenuItemVariant;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface MenuItemVariantMapper {

    MenuItemVariant toEntity(MenuItemVariantDTO dto);

    List<MenuItemVariant> toEntityList(List<MenuItemVariantDTO> dtos);

    MenuItemVariantDTO toDTO(MenuItemVariant variant);

    List<MenuItemVariantDTO> toDTOList(List<MenuItemVariant> variants);

}