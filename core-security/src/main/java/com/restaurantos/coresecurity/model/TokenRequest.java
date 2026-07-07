package com.restaurantos.coresecurity.model;

import com.restaurantos.coresecurity.enums.Audience;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

/**
 * Domain-neutral input for minting a token. Callers (e.g. auth-service) adapt
 * their own user model into this so core-security never depends on any service's
 * domain classes. {@code roles} are canonical names without the {@code ROLE_}
 * prefix.
 */
@Getter
@AllArgsConstructor
public class TokenRequest {

    private final String username;

    private final Set<String> roles;

    private final Set<String> restaurantCodes;

    private final Audience audience;
}
