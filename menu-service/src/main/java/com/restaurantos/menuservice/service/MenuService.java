package com.restaurantos.menuservice.service;

import com.restaurantos.coresecurity.annotation.RestaurantCodes;
import com.restaurantos.menuservice.dto.MenuDTO;

import java.util.List;
import java.util.Set;

public interface MenuService {

    MenuDTO createMenu(MenuDTO request);

    List<MenuDTO> getMenuByRestaurant(@RestaurantCodes Set<String> restaurantCodes);

    MenuDTO updateMenu(String id, MenuDTO request);

    MenuDTO getMenuById(String id);

    List<MenuDTO> getPublishedMenuByRestaurant(@RestaurantCodes Set<String> restaurantCodes);

    MenuDTO publishMenu(String id);

    void deleteMenu(String id);

}
