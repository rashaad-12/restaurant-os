package com.restaurantos.userservice.repository;

import com.restaurantos.userservice.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    @Query(
            value = "SELECT r.* FROM restaurants r " +
                    "JOIN user_restaurants ur ON r.id = ur.user_id " +
                    "JOIN users u ON u.id = ur.restaurant_id " +
                    "WHERE r.code = :code",
            nativeQuery = true
    )
    List<Restaurant> findRestaurantsByUser(@Param("username") String username);

}

