package com.restaurantos.coresecurity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CookieName {

    ACCESS_TOKEN("accessToken"),

    REFRESH_TOKEN("refreshToken"),

    AUTH_TYPE("authType"),

    RESTAURANT_CODES("restaurantCodes"),

    ROLES("roles");

    private final String value;

}
