package com.restaurantos.menuservice.controller;

import com.restaurantos.coresecurity.annotation.RestaurantCodes;
import com.restaurantos.menuservice.dto.MenuDTO;
import com.restaurantos.menuservice.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
public class MenuController {

    @Autowired
    private MenuService menuService;

    @PostMapping
    public ResponseEntity<MenuDTO> createMenu(@RequestBody MenuDTO request) {
        return ResponseEntity.ok(menuService.createMenu(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuDTO> getMenuById(@PathVariable String id) {
        return ResponseEntity.ok(menuService.getMenuById(id));
    }

    @GetMapping("/all")
    public ResponseEntity<List<MenuDTO>> getMenu(@RestaurantCodes Set<String> restaurantCodes) {
        return ResponseEntity.ok(menuService.getMenuByRestaurant(restaurantCodes));
    }

    @GetMapping("/published")
    public ResponseEntity<List<MenuDTO>> getPublishedMenu(@RestaurantCodes Set<String> restaurantCodes) {
        return ResponseEntity.ok(menuService.getPublishedMenuByRestaurant(restaurantCodes));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuDTO> updateMenu(@PathVariable String id, @RequestBody MenuDTO request) {
        return ResponseEntity.ok(menuService.updateMenu(id, request));
    }

    @PostMapping("/{id}/publishMenu")
    public ResponseEntity<MenuDTO> publishMenu(@PathVariable String id) {
        return ResponseEntity.ok(menuService.publishMenu(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenu(@PathVariable String id) {
        menuService.deleteMenu(id);
        return ResponseEntity.noContent().build();
    }
}