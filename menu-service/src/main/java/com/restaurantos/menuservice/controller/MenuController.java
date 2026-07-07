package com.restaurantos.menuservice.controller;

import com.restaurantos.coresecurity.annotation.RestaurantCodes;
import com.restaurantos.menuservice.dto.MenuDTO;
import com.restaurantos.menuservice.service.MenuService;
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
@RequestMapping("/menu-api/v1/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','MANAGER')")
    public ResponseEntity<String> createMenu(@RequestBody List<MenuDTO> request) {
        return ResponseEntity.ok(menuService.createMenu(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuDTO> getMenuById(@PathVariable String id) {
        return ResponseEntity.ok(menuService.getMenuById(id));
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<MenuDTO>> getAllMenu(@RestaurantCodes Set<String> restaurantCodes) {
        return ResponseEntity.ok(menuService.getMenuByRestaurant(restaurantCodes));
    }

    @GetMapping("/published")
    public ResponseEntity<List<MenuDTO>> getPublishedMenu(@RestaurantCodes Set<String> restaurantCodes) {
        return ResponseEntity.ok(menuService.getPublishedMenuByRestaurant(restaurantCodes));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','MANAGER')")
    public ResponseEntity<String> updateMenu(@RequestBody List<MenuDTO> request) {
        return ResponseEntity.ok(menuService.updateMenu(request));
    }

    @PatchMapping("/publish")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','MANAGER')")
    public ResponseEntity<String> publishMenu(@RequestBody List<MenuDTO> request) {
        return ResponseEntity.ok(menuService.publishMenu(request));
    }

    @PatchMapping("/archive")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','MANAGER')")
    public ResponseEntity<String> archiveMenu(@RequestBody List<MenuDTO> request) {
        return ResponseEntity.ok(menuService.archiveMenu(request));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','MANAGER')")
    public ResponseEntity<String> deleteMenu(@RequestBody List<MenuDTO> request) {
        return ResponseEntity.ok(menuService.deleteMenu(request));
    }
}