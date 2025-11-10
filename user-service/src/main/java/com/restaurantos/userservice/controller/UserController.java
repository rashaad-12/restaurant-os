package com.restaurantos.userservice.controller;

import com.restaurantos.coresecurity.annotation.RestaurantCodes;
import com.restaurantos.userservice.dto.UserDTO;
import com.restaurantos.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/user-api/v1/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody List<UserDTO> request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<UserDTO>> getAllUser(@RestaurantCodes Set<String> restaurantCodes) {
        return ResponseEntity.ok(userService.getUserByRestaurant(restaurantCodes));
    }

    @PutMapping
    public ResponseEntity<String> updateUser(@RequestBody List<UserDTO> request) {
        return ResponseEntity.ok(userService.updateUser(request));
    }

    @PatchMapping("/approve")
    public ResponseEntity<String> publishUser(@RequestBody List<UserDTO> request) {
        return ResponseEntity.ok(userService.approveUser(request));
    }

    @PatchMapping("/archive")
    public ResponseEntity<String> archiveUser(@RequestBody List<UserDTO> request) {
        return ResponseEntity.ok(userService.archiveUser(request));
    }

    @DeleteMapping
    public ResponseEntity<String> deleteUser(@RequestBody List<UserDTO> request) {
        return ResponseEntity.ok(userService.deleteUser(request));
    }

}