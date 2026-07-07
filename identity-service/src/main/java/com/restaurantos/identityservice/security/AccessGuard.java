package com.restaurantos.identityservice.security;

import com.restaurantos.coresecurity.model.AuthenticatedUser;
import com.restaurantos.identityservice.user.enums.UserRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Component
public class AccessGuard {

    public AuthenticatedUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new AccessDeniedException("No authenticated principal");
    }

    public boolean isAdmin() {
        return hasAnyRole(UserRole.ADMIN);
    }

    public boolean hasAnyRole(UserRole... roles) {
        Set<String> callerRoles = currentUser().getRoles();
        if (isEmpty(callerRoles)) {
            return false;
        }
        return Arrays.stream(roles).anyMatch(role -> callerRoles.contains(role.name()));
    }

    public void requireAnyRole(UserRole... roles) {
        if (!hasAnyRole(roles)) {
            throw new AccessDeniedException("Requires one of roles " + Arrays.toString(roles));
        }
    }

    public void assertWithinScope(Collection<String> requestedCodes) {
        if (isAdmin()) {
            return;
        }
        Set<String> callerScope = currentUser().getRestaurantCodes();
        if (isEmpty(requestedCodes) || isEmpty(callerScope) || !callerScope.containsAll(requestedCodes)) {
            throw new AccessDeniedException("Request targets restaurant codes outside your scope");
        }
    }

    public void assertCanView(Collection<String> resourceCodes) {
        if (isAdmin()) {
            return;
        }
        Set<String> callerScope = currentUser().getRestaurantCodes();
        if (isEmpty(resourceCodes) || isEmpty(callerScope) || Collections.disjoint(callerScope, resourceCodes)) {
            throw new AccessDeniedException("This resource is outside your scope");
        }
    }

    public void assertNoPrivilegeEscalation(Collection<UserRole> requestedRoles) {
        if (isAdmin() || isEmpty(requestedRoles)) {
            return;
        }
        boolean escalates = requestedRoles.stream()
                .anyMatch(role -> role == UserRole.ADMIN || role == UserRole.OWNER);
        if (escalates) {
            throw new AccessDeniedException("You cannot assign ADMIN or OWNER roles");
        }
    }
}
