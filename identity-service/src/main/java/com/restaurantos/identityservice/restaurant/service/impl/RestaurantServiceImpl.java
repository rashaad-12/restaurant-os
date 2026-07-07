package com.restaurantos.identityservice.restaurant.service.impl;

import com.restaurantos.identityservice.restaurant.dto.RestaurantDTO;
import com.restaurantos.identityservice.restaurant.exception.RestaurantNotFoundException;
import com.restaurantos.identityservice.user.exception.UserNotFoundException;
import com.restaurantos.identityservice.restaurant.mapper.RestaurantMapper;
import com.restaurantos.identityservice.restaurant.model.Restaurant;
import com.restaurantos.identityservice.user.model.User;
import com.restaurantos.identityservice.restaurant.repository.RestaurantRepository;
import com.restaurantos.identityservice.user.repository.UserRepository;
import com.restaurantos.identityservice.restaurant.service.RestaurantService;
import com.restaurantos.identityservice.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.restaurantos.identityservice.user.enums.EntityStatus.ACTIVE;
import static com.restaurantos.identityservice.user.enums.EntityStatus.ARCHIVED;
import static com.restaurantos.identityservice.user.enums.UserRole.ADMIN;
import static com.restaurantos.identityservice.user.enums.UserRole.OWNER;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;

    private final UserRepository userRepository;

    private final RestaurantMapper restaurantMapper;

    private final AccessGuard accessGuard;

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
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(RestaurantNotFoundException::new);

        accessGuard.assertCanView(Set.of(restaurant.getRestaurantCode()));

        return restaurantMapper.toDTO(restaurant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantDTO> getAllRestaurant(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);

        if (user.getRoles().contains(ADMIN)) {
            return restaurantRepository.findAll().stream()
                    .map(restaurantMapper::toDTO)
                    .toList();
        }

        if (user.getRoles().contains(OWNER) && !isEmpty(user.getRestaurantCodes())) {
            return restaurantRepository.findByRestaurantCodeIn(user.getRestaurantCodes()).stream()
                    .map(restaurantMapper::toDTO)
                    .toList();
        }

        return new ArrayList<>();
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

        toUpdate.forEach(existing -> accessGuard.assertWithinScope(Set.of(existing.getRestaurantCode())));

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

        toArchive.forEach(restaurant -> accessGuard.assertWithinScope(Set.of(restaurant.getRestaurantCode())));

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
