package com.restaurantos.identityservice.restaurant.service;

import com.restaurantos.identityservice.restaurant.dto.RestaurantDTO;

import java.util.List;

public interface RestaurantService {

    String createRestaurant(List<RestaurantDTO> request);

    RestaurantDTO getRestaurantById(String id);

    List<RestaurantDTO> getAllRestaurant(String username);

    String updateRestaurant(List<RestaurantDTO> request);

    String approveRestaurant(List<RestaurantDTO> request);

    String archiveRestaurant(List<RestaurantDTO> request);

    String deleteRestaurant(List<RestaurantDTO> request);

}
