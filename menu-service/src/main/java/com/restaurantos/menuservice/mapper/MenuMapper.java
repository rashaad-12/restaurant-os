package com.restaurantos.menuservice.mapper;

import com.restaurantos.menuservice.dto.MenuDTO;
import com.restaurantos.menuservice.enums.MenuStatus;
import com.restaurantos.menuservice.model.Menu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import static java.util.Objects.nonNull;

@Mapper(
        componentModel = "spring",
        uses = { MenuItemMapper.class },
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface MenuMapper {

    @Mapping(target = "status", expression = "java(toStatusOrDefault(request.getStatus()))")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publishDttm", ignore = true)
    Menu toEntity(MenuDTO request);

    MenuDTO toDTO(Menu menu);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createDttm", ignore = true)
    @Mapping(target = "publishDttm", ignore = true)
    @Mapping(target = "items", ignore = true)
    void updateEntityFromDTO(MenuDTO dto, @MappingTarget Menu entity);

    default MenuStatus toStatusOrDefault(MenuStatus status) {
        return nonNull(status) ? status : MenuStatus.DRAFT;
    }

}