package com.restaurantos.orderservice.mapper;

import com.restaurantos.orderservice.dto.OrderItemDTO;
import com.restaurantos.orderservice.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        uses = { StatusMapper.class },
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OrderItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", expression = "java(statusMapper.toStatusOrDefault(dto.getStatus()))")
    OrderItem toEntity(OrderItemDTO dto);

    OrderItemDTO toDTO(OrderItem entity);

}