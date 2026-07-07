package com.restaurantos.identityservice.restaurant.controller;

import com.restaurantos.coresecurity.annotation.CurrentUser;
import com.restaurantos.identityservice.restaurant.dto.RestaurantDTO;
import com.restaurantos.identityservice.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<String> createRestaurant(@RequestBody List<RestaurantDTO> request) {
        return ResponseEntity.ok(restaurantService.createRestaurant(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<RestaurantDTO> getRestaurantById(@PathVariable String id) {
        return ResponseEntity.ok(restaurantService.getRestaurantById(id));
    }

    @GetMapping("/getAll")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<RestaurantDTO>> getAllRestaurant(@CurrentUser String username) {
        return ResponseEntity.ok(restaurantService.getAllRestaurant(username));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<String> updateRestaurant(@RequestBody List<RestaurantDTO> request) {
        return ResponseEntity.ok(restaurantService.updateRestaurant(request));
    }

    @PatchMapping("/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> approveRestaurant(@RequestBody List<RestaurantDTO> request) {
        return ResponseEntity.ok(restaurantService.approveRestaurant(request));
    }

    @PatchMapping("/archive")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<String> archiveRestaurant(@RequestBody List<RestaurantDTO> request) {
        return ResponseEntity.ok(restaurantService.archiveRestaurant(request));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteRestaurant(@RequestBody List<RestaurantDTO> request) {
        return ResponseEntity.ok(restaurantService.deleteRestaurant(request));
    }

}