package com.restaurantos.userservice.controller;

import com.restaurantos.coresecurity.annotation.CurrentUser;
import com.restaurantos.userservice.dto.RestaurantDTO;
import com.restaurantos.userservice.service.RestaurantService;
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

@RestController
@RequestMapping("/restaurant-api/v1/restaurant")
public class RestaurantController {

    @Autowired
    private RestaurantService restaurantService;

    @PostMapping
    public ResponseEntity<String> createRestaurant(@RequestBody List<RestaurantDTO> request) {
        return ResponseEntity.ok(restaurantService.createRestaurant(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantDTO> getRestaurantById(@PathVariable String id) {
        return ResponseEntity.ok(restaurantService.getRestaurantById(id));
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<RestaurantDTO>> getAllRestaurant(@CurrentUser String username) {
        return ResponseEntity.ok(restaurantService.getAllRestaurant(username));
    }

    @PutMapping
    public ResponseEntity<String> updateRestaurant(@RequestBody List<RestaurantDTO> request) {
        return ResponseEntity.ok(restaurantService.updateRestaurant(request));
    }

    @PatchMapping("/publish")
    public ResponseEntity<String> approveRestaurant(@RequestBody List<RestaurantDTO> request) {
        return ResponseEntity.ok(restaurantService.approveRestaurant(request));
    }

    @PatchMapping("/archive")
    public ResponseEntity<String> archiveRestaurant(@RequestBody List<RestaurantDTO> request) {
        return ResponseEntity.ok(restaurantService.archiveRestaurant(request));
    }

    @DeleteMapping
    public ResponseEntity<String> deleteRestaurant(@RequestBody List<RestaurantDTO> request) {
        return ResponseEntity.ok(restaurantService.deleteRestaurant(request));
    }

}