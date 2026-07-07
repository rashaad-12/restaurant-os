package com.restaurantos.identityservice.user.mapper;

import com.restaurantos.identityservice.user.dto.UserDTO;
import com.restaurantos.identityservice.user.enums.EntityStatus;
import com.restaurantos.identityservice.user.model.User;
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
public interface UserMapper {

    @Mapping(target = "id", expression = "java(generateId())")
    @Mapping(target = "status", expression = "java(toStatusOrDefault(dto.getStatus()))")
    User toEntity(UserDTO dto);

    UserDTO toDTO(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createDttm", ignore = true)
    @Mapping(target = "updateDttm", ignore = true)
    void updateEntityFromDTO(UserDTO dto, @MappingTarget User user);

    default String generateId() {
        return UUID.randomUUID().toString();
    }

    default EntityStatus toStatusOrDefault(EntityStatus status) {
        return nonNull(status) ? status : EntityStatus.PENDING_APPROVAL;
    }

}