package com.restaurantos.menuservice.repository;

import com.restaurantos.menuservice.enums.MenuStatus;
import com.restaurantos.menuservice.model.Menu;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MenuRepository extends MongoRepository<Menu, String> {

    List<Menu> findByRestaurantCodeIn(Set<String> restaurantCodes);

    List<Menu> findByRestaurantCodeInAndStatus(Set<String> restaurantCodes, MenuStatus status);

    Optional<Menu> findByNameAndRestaurantCode(String name, String restaurantCode);

    long deleteByNameAndRestaurantCode(String name, String restaurantCode);
}
