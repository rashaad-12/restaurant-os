package com.restaurantos.menuservice.service;

import com.restaurantos.menuservice.dto.MenuDTO;

import java.util.List;
import java.util.Set;

public interface MenuService {

    String createMenu(List<MenuDTO> request);

    MenuDTO getMenuById(String id);

    List<MenuDTO> getMenuByRestaurant(Set<String> restaurantCodes);

    List<MenuDTO> getPublishedMenuByRestaurant(Set<String> restaurantCodes);

    String updateMenu(List<MenuDTO> request);

    String publishMenu(List<MenuDTO> request);

    String archiveMenu(List<MenuDTO> request);

    String deleteMenu(List<MenuDTO> request);

}
