package com.restaurantos.userservice.enums;

import org.springframework.security.core.GrantedAuthority;

public enum UserRole implements GrantedAuthority {

    ADMIN,

    OWNER,

    MANAGER,

    SERVER,

    COOK,

    CUSTOMER,

    DELIVERY_PARTNER;

    @Override
    public String getAuthority() {
        return "ROLE_" + name();
    }
}

