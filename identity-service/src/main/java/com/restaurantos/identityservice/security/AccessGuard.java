package com.restaurantos.identityservice.security;

import com.restaurantos.coresecurity.authz.ScopeGuard;
import com.restaurantos.coresecurity.model.AuthenticatedUser;
import com.restaurantos.identityservice.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Component
@RequiredArgsConstructor
public class AccessGuard {

    private final ScopeGuard scopeGuard;

    public AuthenticatedUser currentUser() {
        return scopeGuard.currentUser();
    }

    public boolean isAdmin() {
        return scopeGuard.isPlatformAdmin();
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
        scopeGuard.assertWithinScope(requestedCodes);
    }

    public void assertCanView(Collection<String> resourceCodes) {
        scopeGuard.assertCanView(resourceCodes);
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
