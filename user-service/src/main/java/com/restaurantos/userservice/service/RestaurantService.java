package com.restaurantos.userservice.service;

import com.restaurantos.userservice.dto.RestaurantDTO;

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
