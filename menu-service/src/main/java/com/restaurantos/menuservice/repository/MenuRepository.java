package com.restaurantos.menuservice.repository;

import com.restaurantos.menuservice.enums.MenuStatus;
import com.restaurantos.menuservice.model.Menu;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface MenuRepository extends MongoRepository<Menu, String> {

    List<Menu> findByRestaurantCodeAndStatus(String restaurantCode, MenuStatus status);

    List<Menu> findByRestaurantCodeInAndStatus(Set<String> restaurantCodes, MenuStatus status);
}
