package com.restaurantos.orderservice.service.impl;

import com.restaurantos.orderservice.dto.OrderDTO;
import com.restaurantos.orderservice.enums.OrderStatus;
import com.restaurantos.orderservice.exception.OrderNotFoundException;
import com.restaurantos.orderservice.mapper.OrderMapper;
import com.restaurantos.orderservice.model.Order;
import com.restaurantos.orderservice.repository.OrderRepository;
import com.restaurantos.orderservice.service.OrderService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.restaurantos.orderservice.enums.OrderStatus.ACCEPTED;
import static com.restaurantos.orderservice.enums.OrderStatus.CANCELLED;
import static com.restaurantos.orderservice.enums.OrderStatus.COMPLETED;
import static com.restaurantos.orderservice.enums.OrderStatus.PLACED;
import static com.restaurantos.orderservice.enums.OrderStatus.PREPARING;
import static com.restaurantos.orderservice.enums.OrderStatus.REJECTED;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderMapper orderMapper;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            PLACED, Set.of(ACCEPTED, REJECTED, CANCELLED),
            ACCEPTED, Set.of(PREPARING, COMPLETED),
            PREPARING, Set.of(COMPLETED, CANCELLED)
    );

    @Override
    @Transactional
    public String createOrder(List<OrderDTO> request) {
        List<Order> orders = request.stream()
                .map(orderMapper::toEntity)
                .peek(order -> order.setOrderItems(order.getOrderItems()))
                .toList();

        orderRepository.saveAll(orders);

        return "Order creation request was processed successfully";
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(orderMapper::toDTO)
                .orElseThrow(OrderNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrder(Set<String> restaurantCodes) {
        return orderRepository.findByRestaurantCodeIn(restaurantCodes).stream()
                .map(orderMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public String updateOrder(List<OrderDTO> request) {
        List<Pair<OrderDTO, Order>> toUpdatePairs = getOrderPairs(request);

        if (toUpdatePairs.isEmpty()) return "No orders to update";

        toUpdatePairs.forEach(orderPair ->
                orderMapper.updateEntityFromDTO(orderPair.getLeft(), orderPair.getRight()));

        List<Order> toUpdate = toUpdatePairs.stream().map(Pair::getRight).toList();
        orderRepository.saveAll(toUpdate);

        return "Order updation request was processed successfully";
    }

    @Override
    @Transactional
    public String acceptOrder(List<OrderDTO> request) {
        List<Pair<OrderDTO, Order>> toAcceptPairs = getOrderPairs(request);

        if (isEmpty(toAcceptPairs)) return "No orders to Accept";

        toAcceptPairs.forEach(orderPair ->
            orderPair.getRight().setStatus(ACCEPTED));

        List<Order> toAccept = toAcceptPairs.stream().map(Pair::getRight).toList();
        orderRepository.saveAll(toAccept);

        return "Order approval request was processed successfully";
    }

    @Override
    @Transactional
    public String prepareOrder(List<OrderDTO> request) {
        List<Pair<OrderDTO, Order>> toPreparePairs = getOrderPairs(request);

        if (isEmpty(toPreparePairs)) return "No orders to publish";

        toPreparePairs.forEach(orderPair ->
                orderPair.getRight().setStatus(PREPARING));

        List<Order> toPrepare = toPreparePairs.stream().map(Pair::getRight).toList();
        orderRepository.saveAll(toPrepare);

        return "Order archive request was processed successfully";
    }

    @Override
    @Transactional
    public String completeOrder(List<OrderDTO> request) {
        List<Pair<OrderDTO, Order>> toCompletePairs = getOrderPairs(request);

        if (isEmpty(toCompletePairs)) return "No orders to publish";

        toCompletePairs.forEach(orderPair ->
                orderPair.getRight().setStatus(COMPLETED));

        List<Order> toComplete = toCompletePairs.stream().map(Pair::getRight).toList();
        orderRepository.saveAll(toComplete);

        return "Order archive request was processed successfully";
    }

    @Override
    @Transactional
    public String rejectOrder(List<OrderDTO> request) {
        List<Pair<OrderDTO, Order>> toRejectPairs = getOrderPairs(request);

        if (isEmpty(toRejectPairs)) return "No orders to publish";

        toRejectPairs.forEach(orderPair ->
                orderPair.getRight().setStatus(REJECTED));

        List<Order> toReject = toRejectPairs.stream().map(Pair::getRight).toList();
        orderRepository.saveAll(toReject);

        return "Order archive request was processed successfully";
    }

    @Override
    @Transactional
    public String cancelOrder(List<OrderDTO> request) {
        List<Pair<OrderDTO, Order>> toCancelPairs = getOrderPairs(request);

        if (isEmpty(toCancelPairs)) return "No orders to publish";

        toCancelPairs.forEach(orderPair ->
                orderPair.getRight().setStatus(CANCELLED));

        List<Order> toCancel = toCancelPairs.stream().map(Pair::getRight).toList();
        orderRepository.saveAll(toCancel);

        return "Order archive request was processed successfully";
    }

    @Override
    @Transactional
    public String deleteOrder(List<OrderDTO> request) {
        request.forEach(orderDTO ->
                orderRepository.deleteByOrderNumberAndRestaurantCode(orderDTO.getOrderNumber(), orderDTO.getRestaurantCode())
        );

        return "Order deletion request was processed successfully";
    }

    private List<Pair<OrderDTO, Order>> getOrderPairs(List<OrderDTO> request) {
        return request.stream()
                .map(dto -> {
                    Order existing = orderRepository.findByOrderNumberAndRestaurantCode(dto.getOrderNumber(), dto.getRestaurantCode())
                            .orElse(null);
                    return Pair.of(dto, existing);
                })
                .filter(orderPair -> nonNull(orderPair.getRight()))
                .filter(this::isValidTransition)
                .toList();
    }

    private boolean isValidTransition(Pair<OrderDTO, Order> orderPair) {
        OrderDTO request = orderPair.getLeft();
        Order existing = orderPair.getRight();

        return ALLOWED_TRANSITIONS.getOrDefault(existing.getStatus(), Set.of())
                .contains(request.getStatus());
    }

}
