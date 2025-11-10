package com.restaurantos.menuservice.mapper;

import com.restaurantos.menuservice.dto.MenuDTO;
import com.restaurantos.menuservice.enums.MenuStatus;
import com.restaurantos.menuservice.model.Menu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.UUID;

import static java.util.Objects.nonNull;

@Mapper(
        componentModel = "spring",
        uses = { MenuItemMapper.class },
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface MenuMapper {

    @Mapping(target = "id", expression = "java(generateId())")
    @Mapping(target = "status", expression = "java(toStatusOrDefault(request.getStatus()))")
    @Mapping(target = "publishDttm", ignore = true)
    @Mapping(target = "createDttm", ignore = true)
    @Mapping(target = "updateDttm", ignore = true)
    Menu toEntity(MenuDTO request);

    MenuDTO toDTO(Menu menu);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createDttm", ignore = true)
    @Mapping(target = "publishDttm", ignore = true)
    void updateEntityFromDTO(MenuDTO dto, @MappingTarget Menu entity);

    default String generateId() {
        return UUID.randomUUID().toString();
    }

    default MenuStatus toStatusOrDefault(String status) {
        try {
            return nonNull(status) ? MenuStatus.valueOf(status) : MenuStatus.DRAFT;
        } catch (IllegalArgumentException e) {
            return MenuStatus.DRAFT;
        }
    }

}