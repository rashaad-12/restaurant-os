package com.restaurantos.menuservice.service.impl;

import com.restaurantos.menuservice.dto.MenuDTO;
import com.restaurantos.menuservice.enums.MenuStatus;
import com.restaurantos.menuservice.exception.MenuNotFoundException;
import com.restaurantos.menuservice.mapper.MenuMapper;
import com.restaurantos.menuservice.model.Menu;
import com.restaurantos.menuservice.repository.MenuRepository;
import com.restaurantos.menuservice.service.MenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Slf4j
@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuMapper menuMapper;

    @Override
    @Transactional
    public String createMenu(List<MenuDTO> request) {
        List<Menu> menus = request.stream()
                .map(menuMapper::toEntity)
                .toList();

        menuRepository.saveAll(menus);

        return "Menu creation request was processed successfully";
    }

    @Override
    @Transactional(readOnly = true)
    public MenuDTO getMenuById(String id) {
        return menuRepository.findById(id)
                .map(menuMapper::toDTO)
                .orElseThrow(MenuNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuDTO> getMenuByRestaurant(Set<String> restaurantCodes) {
        return menuRepository.findByRestaurantCodeIn(restaurantCodes).stream()
                .map(menuMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuDTO> getPublishedMenuByRestaurant(Set<String> restaurantCodes) {
        return menuRepository.findByRestaurantCodeInAndStatus(restaurantCodes, MenuStatus.PUBLISHED).stream()
                .map(menuMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public String updateMenu(List<MenuDTO> request) {
        List<Menu> toUpdate = request.stream()
                .map(menuRequest ->
                        menuRepository.findByNameAndRestaurantCode(menuRequest.getName(), menuRequest.getRestaurantCode())
                                .orElse(null))
                .filter(Objects::nonNull)
                .filter(existing -> !MenuStatus.ARCHIVED.equals(existing.getStatus()))
                .toList();

        if (isEmpty(toUpdate)) return "No menus to publish";

        toUpdate.forEach( existing -> request.stream()
                .filter(menuDTO -> menuDTO.getName().equals(existing.getName())
                        && menuDTO.getRestaurantCode().equals(existing.getRestaurantCode()))
                .findFirst()
                .ifPresent(menuDTO -> {
                    menuMapper.updateEntityFromDTO(menuDTO, existing);
                    if (existing.getStatus().equals(MenuStatus.PUBLISHED))
                        existing.setPublishDttm(LocalDateTime.now());
                    }
                )
        );

        menuRepository.saveAll(toUpdate);

        return "Menu updation request was processed successfully";
    }

    @Override
    @Transactional
    public String publishMenu(List<MenuDTO> request) {
        List<Menu> toPublish = request.stream()
                .map(menuRequest ->
                        menuRepository.findByNameAndRestaurantCode(menuRequest.getName(), menuRequest.getRestaurantCode())
                        .orElse(null))
                .filter(Objects::nonNull)
                .filter(menu -> !MenuStatus.PUBLISHED.equals(menu.getStatus()))
                .toList();

        if (isEmpty(toPublish)) return "No menus to publish";

        toPublish.forEach(menu -> {
            menu.setStatus(MenuStatus.PUBLISHED);
            menu.setPublishDttm(LocalDateTime.now());
        });

        menuRepository.saveAll(toPublish);

        return "Menu publish request was processed successfully";
    }

    @Override
    @Transactional
    public String archiveMenu(List<MenuDTO> request) {
        List<Menu> toArchive = request.stream()
                .map(menuRequest ->
                        menuRepository.findByNameAndRestaurantCode(menuRequest.getName(), menuRequest.getRestaurantCode())
                                .orElse(null))
                .filter(Objects::nonNull)
                .filter(menu -> !MenuStatus.ARCHIVED.equals(menu.getStatus()))
                .toList();

        if (isEmpty(toArchive)) return "No menus to publish";

        toArchive.forEach(menu ->
            menu.setStatus(MenuStatus.ARCHIVED)
        );

        menuRepository.saveAll(toArchive);

        return "Menu archive request was processed successfully";
    }

    @Override
    @Transactional
    public String deleteMenu(List<MenuDTO> request) {
        request.forEach(menuDTO ->
                menuRepository.deleteByNameAndRestaurantCode(menuDTO.getName(), menuDTO.getRestaurantCode())
        );

        return "Menu deletion request was processed successfully";
    }

}
