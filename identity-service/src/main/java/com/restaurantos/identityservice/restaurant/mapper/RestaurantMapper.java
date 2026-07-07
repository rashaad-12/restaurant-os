package com.restaurantos.identityservice.restaurant.mapper;

import com.restaurantos.identityservice.restaurant.dto.RestaurantDTO;
import com.restaurantos.identityservice.user.enums.EntityStatus;
import com.restaurantos.identityservice.restaurant.model.Restaurant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.UUID;

import static java.util.Objects.nonNull;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface RestaurantMapper {

    @Mapping(target = "id", expression = "java(generateId())")
    @Mapping(target = "status", expression = "java(toStatusOrDefault(dto.getStatus()))")
    Restaurant toEntity(RestaurantDTO dto);

    RestaurantDTO toDTO(Restaurant restaurant);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createDttm", ignore = true)
    @Mapping(target = "updateDttm", ignore = true)
    void updateEntityFromDTO(RestaurantDTO dto, @MappingTarget Restaurant restaurant);

    default String generateId() {
        return UUID.randomUUID().toString();
    }

    default EntityStatus toStatusOrDefault(EntityStatus status) {
        return nonNull(status) ? status : EntityStatus.PENDING_APPROVAL;
    }

}