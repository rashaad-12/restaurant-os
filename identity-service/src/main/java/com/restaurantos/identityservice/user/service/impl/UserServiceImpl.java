package com.restaurantos.identityservice.user.service.impl;

import com.restaurantos.identityservice.user.dto.UserDTO;
import com.restaurantos.identityservice.user.exception.UserNotFoundException;
import com.restaurantos.identityservice.user.model.User;
import com.restaurantos.identityservice.user.repository.UserRepository;
import com.restaurantos.identityservice.user.service.UserService;
import com.restaurantos.identityservice.user.mapper.UserMapper;
import com.restaurantos.identityservice.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.restaurantos.identityservice.user.enums.EntityStatus.ACTIVE;
import static com.restaurantos.identityservice.user.enums.EntityStatus.ARCHIVED;
import static com.restaurantos.identityservice.user.enums.UserRole.ADMIN;
import static com.restaurantos.identityservice.user.enums.UserRole.MANAGER;
import static com.restaurantos.identityservice.user.enums.UserRole.OWNER;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final AccessGuard accessGuard;

    @Override
    @Transactional
    public String createUser(List<UserDTO> request) {
        request.forEach(dto -> {
            accessGuard.assertNoPrivilegeEscalation(dto.getRoles());
            accessGuard.assertWithinScope(dto.getRestaurantCodes());
        });

        List<User> users = request.stream()
                .map(userMapper::toEntity)
                .peek(user -> {
                    if (isNotBlank(user.getPassword()))
                        user.setPassword(passwordEncoder.encode(user.getPassword()));
                })
                .toList();

        userRepository.saveAll(users);

        return "User creation request was processed successfully";
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(String id) {
        User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);

        boolean self = user.getUsername().equals(accessGuard.currentUser().getUsername());
        if (!self) {
            accessGuard.requireAnyRole(ADMIN, OWNER, MANAGER);
            accessGuard.assertCanView(user.getRestaurantCodes());
        }

        return userMapper.toDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getUserByRestaurant(Set<String> restaurantCodes) {
        List<User> users = accessGuard.isAdmin()
                ? userRepository.findAll()
                : userRepository.findByRestaurantCodesIn(restaurantCodes);

        return users.stream()
                .map(userMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public String updateUser(List<UserDTO> request) {
        List<User> toUpdate = request.stream()
                .map(userRequest ->
                        userRepository.findByUsername(userRequest.getUsername())
                                .orElse(null))
                .filter(Objects::nonNull)
                .filter(existing -> !ARCHIVED.equals(existing.getStatus()))
                .toList();

        if (isEmpty(toUpdate)) return "No users to publish";

        toUpdate.forEach(existing -> accessGuard.assertWithinScope(existing.getRestaurantCodes()));
        request.forEach(dto -> {
            accessGuard.assertNoPrivilegeEscalation(dto.getRoles());
            if (!isEmpty(dto.getRestaurantCodes())) accessGuard.assertWithinScope(dto.getRestaurantCodes());
        });

        toUpdate.forEach( existing -> request.stream()
                .filter(userDTO -> userDTO.getUsername().equals(existing.getUsername()))
                .findFirst()
                .ifPresent(userDTO -> userMapper.updateEntityFromDTO(userDTO, existing))
        );

        userRepository.saveAll(toUpdate);

        return "User updation request was processed successfully";
    }

    @Override
    @Transactional
    public String approveUser(List<UserDTO> request) {
        List<User> toApprove = request.stream()
                .map(userRequest ->
                        userRepository.findByUsername(userRequest.getUsername())
                                .orElse(null))
                .filter(Objects::nonNull)
                .filter(user -> !ACTIVE.equals(user.getStatus()))
                .toList();

        if (isEmpty(toApprove)) return "No users to approve";

        toApprove.forEach(user -> accessGuard.assertWithinScope(user.getRestaurantCodes()));

        toApprove.forEach(user -> {
            user.setStatus(ACTIVE);
        });

        userRepository.saveAll(toApprove);

        return "User approval request was processed successfully";
    }

    @Override
    @Transactional
    public String archiveUser(List<UserDTO> request) {
        List<User> toArchive = request.stream()
                .map(userRequest ->
                        userRepository.findByUsername(userRequest.getUsername())
                                .orElse(null))
                .filter(Objects::nonNull)
                .filter(user -> !ARCHIVED.equals(user.getStatus()))
                .toList();

        if (isEmpty(toArchive)) return "No users to publish";

        toArchive.forEach(user -> accessGuard.assertWithinScope(user.getRestaurantCodes()));

        toArchive.forEach(user ->
                user.setStatus(ARCHIVED)
        );

        userRepository.saveAll(toArchive);

        return "User archive request was processed successfully";
    }

    @Override
    @Transactional
    public String deleteUser(List<UserDTO> request) {
        List<User> toDelete = request.stream()
                .map(userDTO -> userRepository.findByUsername(userDTO.getUsername()).orElse(null))
                .filter(Objects::nonNull)
                .toList();

        toDelete.forEach(user -> accessGuard.assertWithinScope(user.getRestaurantCodes()));

        toDelete.forEach(user -> userRepository.deleteByUsername(user.getUsername()));

        return "User deletion request was processed successfully";
    }

}
