package com.restaurantos.orderservice.mapper;

import com.restaurantos.orderservice.dto.OrderDTO;
import com.restaurantos.orderservice.model.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        uses = { StatusMapper.class, OrderItemMapper.class },
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "status", expression = "java(statusMapper.toStatusOrDefault(dto.getStatus()))")
    Order toEntity(OrderDTO dto);

    OrderDTO toDTO(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "createDttm", ignore = true)
    @Mapping(target = "updateDttm", ignore = true)
    void updateEntityFromDTO(OrderDTO dto, @MappingTarget Order order);

}