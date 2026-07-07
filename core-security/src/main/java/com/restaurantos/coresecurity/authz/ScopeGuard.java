package com.restaurantos.coresecurity.authz;

import com.restaurantos.coresecurity.model.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Component
public class ScopeGuard {

    public static final String PLATFORM_ADMIN_ROLE = "ADMIN";

    public AuthenticatedUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new AccessDeniedException("No authenticated principal");
    }

    public boolean hasRole(String role) {
        Set<String> roles = currentUser().getRoles();
        return roles != null && roles.contains(role);
    }

    public boolean isPlatformAdmin() {
        return hasRole(PLATFORM_ADMIN_ROLE);
    }

    public boolean hasRestaurantScope() {
        return !isEmpty(currentUser().getRestaurantCodes());
    }

    public Set<String> callerScope() {
        Set<String> scope = currentUser().getRestaurantCodes();
        return scope == null ? Set.of() : scope;
    }

    public void assertWithinScope(Collection<String> requestedCodes) {
        if (isPlatformAdmin()) {
            return;
        }
        Set<String> callerScope = currentUser().getRestaurantCodes();
        if (isEmpty(requestedCodes) || isEmpty(callerScope) || !callerScope.containsAll(requestedCodes)) {
            throw new AccessDeniedException("Request targets restaurant codes outside your scope");
        }
    }

    public void assertCanView(Collection<String> resourceCodes) {
        if (isPlatformAdmin()) {
            return;
        }
        Set<String> callerScope = currentUser().getRestaurantCodes();
        if (isEmpty(resourceCodes) || isEmpty(callerScope) || Collections.disjoint(callerScope, resourceCodes)) {
            throw new AccessDeniedException("This resource is outside your scope");
        }
    }
}
