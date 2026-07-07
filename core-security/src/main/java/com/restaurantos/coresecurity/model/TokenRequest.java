package com.restaurantos.coresecurity.model;

import com.restaurantos.coresecurity.enums.Audience;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class TokenRequest {

    private final String username;

    private final Set<String> roles;

    private final Set<String> restaurantCodes;

    private final Audience audience;
}
