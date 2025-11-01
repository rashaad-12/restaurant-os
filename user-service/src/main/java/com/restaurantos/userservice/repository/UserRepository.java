package com.restaurantos.userservice.repository;

import com.restaurantos.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query(
            value = "SELECT u.* FROM users u " +
                    "JOIN user_restaurants ur ON u.id = ur.user_id " +
                    "JOIN restaurants r ON r.id = ur.restaurant_id " +
                    "WHERE r.code = :code",
            nativeQuery = true
    )
    List<User> findUsersByRestaurant(@Param("code") String code);

}

