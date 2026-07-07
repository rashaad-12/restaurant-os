package com.restaurantos.identityservice.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.restaurantos.identityservice.user.enums.EntityStatus;
import com.restaurantos.identityservice.user.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private String username;

    @JsonProperty(access = WRITE_ONLY)
    private String password;

    private String firstName;

    private String lastName;

    private Set<UserRole> roles;

    private String oauthProvider;

    private Set<String> restaurantCodes;

    private Set<String> restaurantGroups;

    private EntityStatus status;

    private String createdBy;

    private LocalDateTime createDttm;

    private LocalDateTime updateDttm;
}