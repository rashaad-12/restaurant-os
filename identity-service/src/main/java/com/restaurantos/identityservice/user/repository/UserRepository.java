package com.restaurantos.identityservice.user.repository;

import com.restaurantos.identityservice.user.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    List<User> findByRestaurantCodesIn(Set<String> restaurantCodes);

    void deleteByUsername(String username);

}

