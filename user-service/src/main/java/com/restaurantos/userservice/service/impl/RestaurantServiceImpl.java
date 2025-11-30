package com.restaurantos.userservice.service.impl;

import com.restaurantos.userservice.dto.RestaurantDTO;
import com.restaurantos.userservice.exception.RestaurantNotFoundException;
import com.restaurantos.userservice.exception.UserNotFoundException;
import com.restaurantos.userservice.mapper.RestaurantMapper;
import com.restaurantos.userservice.model.Restaurant;
import com.restaurantos.userservice.model.User;
import com.restaurantos.userservice.repository.RestaurantRepository;
import com.restaurantos.userservice.repository.UserRepository;
import com.restaurantos.userservice.service.RestaurantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.restaurantos.userservice.enums.EntityStatus.ACTIVE;
import static com.restaurantos.userservice.enums.EntityStatus.ARCHIVED;
import static com.restaurantos.userservice.enums.UserRole.ADMIN;
import static com.restaurantos.userservice.enums.UserRole.OWNER;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Service
public class RestaurantServiceImpl implements RestaurantService {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantMapper restaurantMapper;

    @Override
    @Transactional
    public String createRestaurant(List<RestaurantDTO> request) {
        List<Restaurant> restaurants = request.stream()
                .map(restaurantMapper::toEntity)
                .toList();

        restaurantRepository.saveAll(restaurants);

        return "Restaurant creation request was processed successfully";
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantDTO getRestaurantById(String id) {
        return restaurantRepository.findById(id)
                .map(restaurantMapper::toDTO)
                .orElseThrow(RestaurantNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantDTO> getAllRestaurant(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);

        if (Collections.disjoint(user.getRoles(), Set.of(ADMIN, OWNER))) {
            return new ArrayList<>();
        }

        return restaurantRepository.findAll().stream()
                .map(restaurantMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public String updateRestaurant(List<RestaurantDTO> request) {
        List<Restaurant> toUpdate = request.stream()
                .map(restaurantRequest ->
                        restaurantRepository.findByRestaurantCode(restaurantRequest.getRestaurantCode())
                                .orElse(null))
                .filter(Objects::nonNull)
                .filter(existing -> !ARCHIVED.equals(existing.getStatus()))
                .toList();

        if (isEmpty(toUpdate)) return "No restaurants to publish";

        toUpdate.forEach( existing -> request.stream()
                .filter(restaurantDTO -> restaurantDTO.getRestaurantCode().equals(existing.getRestaurantCode()))
                .findFirst()
                .ifPresent(restaurantDTO -> restaurantMapper.updateEntityFromDTO(restaurantDTO, existing))
        );

        restaurantRepository.saveAll(toUpdate);

        return "Restaurant updation request was processed successfully";
    }

    @Override
    @Transactional
    public String approveRestaurant(List<RestaurantDTO> request) {
        List<Restaurant> toApprove = request.stream()
                .map(restaurantRequest ->
                        restaurantRepository.findByRestaurantCode(restaurantRequest.getRestaurantCode())
                                .orElse(null))
                .filter(Objects::nonNull)
                .filter(restaurant -> !ACTIVE.equals(restaurant.getStatus()))
                .toList();

        if (isEmpty(toApprove)) return "No restaurants to approve";

        toApprove.forEach(restaurant -> {
            restaurant.setStatus(ACTIVE);
        });

        restaurantRepository.saveAll(toApprove);

        return "Restaurant approval request was processed successfully";
    }

    @Override
    @Transactional
    public String archiveRestaurant(List<RestaurantDTO> request) {
        List<Restaurant> toArchive = request.stream()
                .map(restaurantRequest ->
                        restaurantRepository.findByRestaurantCode(restaurantRequest.getRestaurantCode())
                                .orElse(null))
                .filter(Objects::nonNull)
                .filter(restaurant -> !ARCHIVED.equals(restaurant.getStatus()))
                .toList();

        if (isEmpty(toArchive)) return "No restaurants to publish";

        toArchive.forEach(restaurant ->
                restaurant.setStatus(ARCHIVED)
        );

        restaurantRepository.saveAll(toArchive);

        return "Restaurant archive request was processed successfully";
    }

    @Override
    @Transactional
    public String deleteRestaurant(List<RestaurantDTO> request) {
        request.forEach(restaurantDTO ->
                restaurantRepository.deleteByRestaurantCode(restaurantDTO.getRestaurantCode())
        );

        return "Restaurant deletion request was processed successfully";
    }
    
}
