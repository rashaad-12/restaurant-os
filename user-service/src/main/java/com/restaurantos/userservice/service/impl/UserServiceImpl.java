package com.restaurantos.userservice.service.impl;

import com.restaurantos.userservice.dto.UserDTO;
import com.restaurantos.userservice.exception.UserNotFoundException;
import com.restaurantos.userservice.model.User;
import com.restaurantos.userservice.repository.UserRepository;
import com.restaurantos.userservice.service.UserService;
import com.restaurantos.userservice.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.restaurantos.userservice.enums.EntityStatus.ACTIVE;
import static com.restaurantos.userservice.enums.EntityStatus.ARCHIVED;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public String createUser(List<UserDTO> request) {
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
        return userRepository.findById(id)
                .map(userMapper::toDTO)
                .orElseThrow(UserNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getUserByRestaurant(Set<String> restaurantCodes) {
        return userRepository.findByRestaurantCodesIn(restaurantCodes).stream()
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

        toArchive.forEach(user ->
                user.setStatus(ARCHIVED)
        );

        userRepository.saveAll(toArchive);

        return "User archive request was processed successfully";
    }

    @Override
    @Transactional
    public String deleteUser(List<UserDTO> request) {
        request.forEach(userDTO ->
                userRepository.deleteByUsername(userDTO.getUsername())
        );

        return "User deletion request was processed successfully";
    }

}
