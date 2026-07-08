package com.restaurantos.orderservice.service;

import com.restaurantos.orderservice.dto.OrderDTO;
import com.restaurantos.orderservice.dto.search.SearchDocument;

import java.util.List;
import java.util.Set;

public interface OrderService {

    String createOrder(List<OrderDTO> request);

    OrderDTO getOrderById(Long id);

    List<SearchDocument> getSearchDocuments(List<String> ids);

    List<OrderDTO> getAllOrder(Set<String> restaurantCodes);

    String updateOrder(List<OrderDTO> request);

    String acceptOrder(List<OrderDTO> request);

    String prepareOrder(List<OrderDTO> request);

    String completeOrder(List<OrderDTO> request);

    String rejectOrder(List<OrderDTO> request);

    String cancelOrder(List<OrderDTO> request);

    String deleteOrder(List<OrderDTO> request);

}
