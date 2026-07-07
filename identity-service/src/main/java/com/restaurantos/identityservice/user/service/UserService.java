package com.restaurantos.identityservice.user.service;

import com.restaurantos.identityservice.user.dto.UserDTO;

import java.util.List;
import java.util.Set;

public interface UserService {

    String createUser(List<UserDTO> request);

    UserDTO getUserById(String id);

    List<UserDTO> getUserByRestaurant(Set<String> restaurantCodes);

    String updateUser(List<UserDTO> request);

    String approveUser(List<UserDTO> request);

    String archiveUser(List<UserDTO> request);

    String deleteUser(List<UserDTO> request);
    
}