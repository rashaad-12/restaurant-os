package com.restaurantos.identityservice.restaurant.repository;

import com.restaurantos.identityservice.restaurant.model.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends MongoRepository<Restaurant, String> {

    Optional<Restaurant> findByRestaurantCode(String restaurantCode);

    List<Restaurant> findByRestaurantCodeIn(Collection<String> restaurantCodes);

    void deleteByRestaurantCode(String restaurantCode);

}

