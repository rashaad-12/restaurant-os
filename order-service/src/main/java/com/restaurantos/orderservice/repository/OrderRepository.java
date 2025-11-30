package com.restaurantos.orderservice.repository;

import com.restaurantos.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByRestaurantCodeIn(Set<String> restaurantCodes);

    Optional<Order> findByOrderNumberAndRestaurantCode(String orderNumber, String restaurantCode);

    void deleteByOrderNumberAndRestaurantCode(String orderNumber, String restaurantCode);
}

