package com.restaurantos.userservice.repository;

import com.restaurantos.userservice.model.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RestaurantRepository extends MongoRepository<Restaurant, String> {

    Optional<Restaurant> findByRestaurantCode(String restaurantCode);

    void deleteByRestaurantCode(String restaurantCode);

}

