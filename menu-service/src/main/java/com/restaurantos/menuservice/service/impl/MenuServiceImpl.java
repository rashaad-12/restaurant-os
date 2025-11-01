package com.restaurantos.menuservice.service.impl;

import com.restaurantos.menuservice.dto.MenuDTO;
import com.restaurantos.menuservice.enums.MenuStatus;
import com.restaurantos.menuservice.exception.MenuNotFoundException;
import com.restaurantos.menuservice.mapper.MenuMapper;
import com.restaurantos.menuservice.model.Menu;
import com.restaurantos.menuservice.repository.MenuRepository;
import com.restaurantos.menuservice.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuMapper menuMapper;

    @Override
    public MenuDTO createMenu(MenuDTO request) {
        Menu menu = menuMapper.toMenuEntity(request);
        return menuMapper.toMenuDTO(menuRepository.save(menu));
    }

    @Override
    public MenuDTO getMenuById(String id) {
        return menuRepository.findById(id)
                .map(menuMapper::toMenuDTO)
                .orElseThrow(MenuNotFoundException::new);
    }

    @Override
    public List<MenuDTO> getPublishedMenuByRestaurant(Set<String> restaurantCodes) {
        return menuRepository.findByRestaurantCodeInAndStatus(restaurantCodes, MenuStatus.PUBLISHED)
                .stream().map(menuMapper::toMenuDTO).toList();
    }

    @Override
    public List<MenuDTO> getMenuByRestaurant(Set<String> restaurantCodes) {
        return menuRepository.findByRestaurantCodeInAndStatus(restaurantCodes, MenuStatus.PUBLISHED)
                .stream().map(menuMapper::toMenuDTO).toList();
    }

    @Override
    public MenuDTO updateMenu(String id, MenuDTO request) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(MenuNotFoundException::new);
        menu.setName(request.getName());
        menu.setDescription(request.getDescription());
        menu.setItems(menuMapper.toMenuItemEntityList(request.getItems()));
        return menuMapper.toMenuDTO(menuRepository.save(menu));
    }

    @Override
    public MenuDTO publishMenu(String id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(MenuNotFoundException::new);

        if (menu.getStatus() == MenuStatus.PUBLISHED) {
            return menuMapper.toMenuDTO(menu);
        }

        menu.setStatus(MenuStatus.PUBLISHED);
        menu.setPublishDttm(LocalDateTime.now());
        return menuMapper.toMenuDTO(menuRepository.save(menu));
    }

    @Override
    public void deleteMenu(String id) {
        menuRepository.deleteById(id);
    }
}
