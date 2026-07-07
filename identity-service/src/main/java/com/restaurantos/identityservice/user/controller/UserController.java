package com.restaurantos.identityservice.user.controller;

import com.restaurantos.coresecurity.annotation.RestaurantCodes;
import com.restaurantos.identityservice.user.dto.UserDTO;
import com.restaurantos.identityservice.user.service.UserService;
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
import java.util.Set;

@RestController
@RequestMapping("/user-api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','MANAGER')")
    public ResponseEntity<String> createUser(@RequestBody List<UserDTO> request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/getAll")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','MANAGER')")
    public ResponseEntity<List<UserDTO>> getAllUser(@RestaurantCodes Set<String> restaurantCodes) {
        return ResponseEntity.ok(userService.getUserByRestaurant(restaurantCodes));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','MANAGER')")
    public ResponseEntity<String> updateUser(@RequestBody List<UserDTO> request) {
        return ResponseEntity.ok(userService.updateUser(request));
    }

    @PatchMapping("/approve")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','MANAGER')")
    public ResponseEntity<String> publishUser(@RequestBody List<UserDTO> request) {
        return ResponseEntity.ok(userService.approveUser(request));
    }

    @PatchMapping("/archive")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','MANAGER')")
    public ResponseEntity<String> archiveUser(@RequestBody List<UserDTO> request) {
        return ResponseEntity.ok(userService.archiveUser(request));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<String> deleteUser(@RequestBody List<UserDTO> request) {
        return ResponseEntity.ok(userService.deleteUser(request));
    }

}