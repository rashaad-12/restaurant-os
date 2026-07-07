package com.restaurantos.coresecurity.model;

import com.restaurantos.coresecurity.enums.Audience;
import com.restaurantos.coresecurity.enums.TokenType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;
import java.util.Set;

@Getter
@AllArgsConstructor
public class AuthenticatedUser implements Principal {

    private final String username;

    private final Set<String> roles;

    private final Set<String> restaurantCodes;

    private final Audience audience;

    private final TokenType tokenType;

    private final String tokenId;

    @Override
    public String getName() {
        return username;
    }
}
