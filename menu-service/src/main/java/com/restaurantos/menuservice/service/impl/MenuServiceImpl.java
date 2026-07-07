package com.restaurantos.menuservice.service.impl;

import com.restaurantos.common.util.DateUtil;
import com.restaurantos.coresecurity.authz.ScopeGuard;
import com.restaurantos.menuservice.dto.MenuDTO;
import com.restaurantos.menuservice.dto.MenuItemDTO;
import com.restaurantos.menuservice.enums.MenuStatus;
import com.restaurantos.menuservice.exception.MenuNotFoundException;
import com.restaurantos.menuservice.mapper.MenuItemMapper;
import com.restaurantos.menuservice.mapper.MenuMapper;
import com.restaurantos.menuservice.model.Menu;
import com.restaurantos.menuservice.model.MenuItem;
import com.restaurantos.menuservice.repository.MenuRepository;
import com.restaurantos.menuservice.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.restaurantos.menuservice.enums.MenuStatus.PUBLISHED;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;

    private final MenuMapper menuMapper;

    private final MenuItemMapper menuItemMapper;

    private final ScopeGuard scopeGuard;

    @Override
    @Transactional
    public String createMenu(List<MenuDTO> request) {
        request.forEach(dto -> scopeGuard.assertWithinScope(Set.of(dto.getRestaurantCode())));

        List<Menu> menus = request.stream()
                .map(menuMapper::toEntity)
                .toList();

        menus.forEach(menu -> {
            if(menu.getStatus().equals(PUBLISHED)) {
                menu.setStatus(PUBLISHED);
                menu.setPublishDttm(DateUtil.getCurrentDateTime());
            }
        });

        menuRepository.saveAll(menus);

        return "Menu creation request was processed successfully";
    }

    @Override
    @Transactional(readOnly = true)
    public MenuDTO getMenuById(String id) {
        Menu menu = menuRepository.findById(id).orElseThrow(MenuNotFoundException::new);

        scopeGuard.assertCanView(Set.of(menu.getRestaurantCode()));

        return menuMapper.toDTO(menu);
    }

    @Override
    @Transactional(readOnly = true)
    public MenuDTO getMenuByCodeAndRestaurantCode(String menuCode, String restaurantCode) {
        return menuRepository.findByCodeAndRestaurantCode(menuCode, restaurantCode)
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
        return menuRepository.findByRestaurantCodeInAndStatus(restaurantCodes, PUBLISHED).stream()
                .map(menuMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public String updateMenu(List<MenuDTO> request) {
        request.forEach(dto -> scopeGuard.assertWithinScope(Set.of(dto.getRestaurantCode())));

        List<Menu> toUpdate = request.stream()
                .map(menuRequest ->
                        menuRepository.findByCodeAndRestaurantCode(menuRequest.getCode(), menuRequest.getRestaurantCode())
                                .orElse(null))
                .filter(Objects::nonNull)
                .filter(existing -> !MenuStatus.ARCHIVED.equals(existing.getStatus()))
                .toList();

        if (isEmpty(toUpdate)) return "No menus to publish";

        toUpdate.forEach( existing -> request.stream()
                .filter(menuDTO -> menuDTO.getCode().equals(existing.getCode())
                        && menuDTO.getRestaurantCode().equals(existing.getRestaurantCode()))
                .findFirst()
                .ifPresent(menuDTO -> {
                    menuMapper.updateEntityFromDTO(menuDTO, existing);
                    updateAndMergeMenuItems(existing, menuDTO);
                    if (existing.getStatus().equals(PUBLISHED))
                        existing.setPublishDttm(DateUtil.getCurrentDateTime());
                    }
                )
        );

        menuRepository.saveAll(toUpdate);

        return "Menu updation request was processed successfully";
    }

    @Override
    @Transactional
    public String publishMenu(List<MenuDTO> request) {
        request.forEach(dto -> scopeGuard.assertWithinScope(Set.of(dto.getRestaurantCode())));

        List<Menu> toPublish = request.stream()
                .map(menuRequest ->
                        menuRepository.findByCodeAndRestaurantCode(menuRequest.getCode(), menuRequest.getRestaurantCode())
                        .orElse(null))
                .filter(Objects::nonNull)
                .filter(menu -> !PUBLISHED.equals(menu.getStatus()))
                .toList();

        if (isEmpty(toPublish)) return "No menus to publish";

        toPublish.forEach(menu -> {
            menu.setStatus(PUBLISHED);
            menu.setPublishDttm(DateUtil.getCurrentDateTime());
        });

        menuRepository.saveAll(toPublish);

        return "Menu publish request was processed successfully";
    }

    @Override
    @Transactional
    public String archiveMenu(List<MenuDTO> request) {
        request.forEach(dto -> scopeGuard.assertWithinScope(Set.of(dto.getRestaurantCode())));

        List<Menu> toArchive = request.stream()
                .map(menuRequest ->
                        menuRepository.findByCodeAndRestaurantCode(menuRequest.getCode(), menuRequest.getRestaurantCode())
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
        request.forEach(dto -> scopeGuard.assertWithinScope(Set.of(dto.getRestaurantCode())));

        request.forEach(menuDTO ->
                menuRepository.deleteByCodeAndRestaurantCode(menuDTO.getCode(), menuDTO.getRestaurantCode())
        );

        return "Menu deletion request was processed successfully";
    }

    private void updateAndMergeMenuItems(Menu existing, MenuDTO dto) {
        if (isNull(dto.getItems())) return;

        Map<String, MenuItem> existingMap = existing.getItems().stream()
                .collect(toMap(MenuItem::getCode, i -> i));

        for (MenuItemDTO menuItem : dto.getItems()) {
            if (menuItem.isMarkedForDeletion())
                existing.getItems().removeIf(item -> item.getCode().equals(menuItem.getCode()));

            MenuItem match = existingMap.get(menuItem.getCode());
            if (nonNull(match)) {
                menuItemMapper.updateEntityFromDTO(menuItem, match);
            } else {
                existing.getItems().add(menuItemMapper.toEntity(menuItem));
            }
        }
    }

}
