package com.restaurantos.coresecurity.util;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public final class SecurityAuthorities {

    private static final String ROLE_PREFIX = "ROLE_";

    private SecurityAuthorities() {
    }

    public static Set<SimpleGrantedAuthority> toAuthorities(Collection<String> roles) {
        if (isEmpty(roles)) return Set.of();

        return roles.stream()
                .map(SecurityAuthorities::normalise)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    private static String normalise(String role) {
        return role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
    }
}
