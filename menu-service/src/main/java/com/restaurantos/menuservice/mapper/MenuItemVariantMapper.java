package com.restaurantos.menuservice.mapper;

import com.restaurantos.menuservice.dto.MenuItemVariantDTO;
import com.restaurantos.menuservice.model.MenuItemVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.UUID;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface MenuItemVariantMapper {

    @Mapping(target = "id", expression = "java(generateId())")
    MenuItemVariant toEntity(MenuItemVariantDTO dto);

    List<MenuItemVariant> toEntityList(List<MenuItemVariantDTO> dtos);

    MenuItemVariantDTO toDTO(MenuItemVariant variant);

    List<MenuItemVariantDTO> toDTOList(List<MenuItemVariant> variants);

    default String generateId() {
        return UUID.randomUUID().toString();
    }

}