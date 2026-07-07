package com.restaurantos.coresecurity.resolver;

import com.restaurantos.coresecurity.model.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.Optional;

import static java.util.Objects.nonNull;

public abstract class AbstractPrincipalArgumentResolver implements HandlerMethodArgumentResolver {
    protected Optional<AuthenticatedUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (nonNull(authentication) && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
