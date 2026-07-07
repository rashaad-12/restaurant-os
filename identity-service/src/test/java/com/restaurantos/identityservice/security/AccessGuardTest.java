package com.restaurantos.identityservice.security;

import com.restaurantos.coresecurity.enums.Audience;
import com.restaurantos.coresecurity.enums.TokenType;
import com.restaurantos.coresecurity.model.AuthenticatedUser;
import com.restaurantos.identityservice.user.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessGuardTest {

    private final AccessGuard guard = new AccessGuard();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Set<String> roles, Set<String> restaurantCodes) {
        AuthenticatedUser principal = new AuthenticatedUser(
                "caller@example.com", roles, restaurantCodes, Audience.STAFF, TokenType.ACCESS, "tid");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, Set.of()));
    }

    @Test
    void nonAdmin_withinScope_isAllowed() {
        authenticateAs(Set.of("MANAGER"), Set.of("R1", "R2"));

        assertThatCode(() -> guard.assertWithinScope(Set.of("R1"))).doesNotThrowAnyException();
    }

    @Test
    void nonAdmin_outsideScope_isDenied() {
        authenticateAs(Set.of("MANAGER"), Set.of("R1"));

        assertThatThrownBy(() -> guard.assertWithinScope(Set.of("R2")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void nonAdmin_emptyRequest_isDenied() {
        authenticateAs(Set.of("MANAGER"), Set.of("R1"));

        assertThatThrownBy(() -> guard.assertWithinScope(Set.of()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void admin_bypassesScope() {
        authenticateAs(Set.of("ADMIN"), Set.of());

        assertThatCode(() -> guard.assertWithinScope(Set.of("R1", "R99"))).doesNotThrowAnyException();
    }

    @Test
    void canView_intersectingScope_isAllowed() {
        authenticateAs(Set.of("MANAGER"), Set.of("R1", "R2"));

        assertThatCode(() -> guard.assertCanView(Set.of("R2", "R3"))).doesNotThrowAnyException();
    }

    @Test
    void canView_disjointScope_isDenied() {
        authenticateAs(Set.of("MANAGER"), Set.of("R1"));

        assertThatThrownBy(() -> guard.assertCanView(Set.of("R2")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void canView_resourceWithNoScope_isDeniedForNonAdmin() {
        authenticateAs(Set.of("MANAGER"), Set.of("R1"));

        assertThatThrownBy(() -> guard.assertCanView(Set.of()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void canView_admin_seesEverything() {
        authenticateAs(Set.of("ADMIN"), Set.of());

        assertThatCode(() -> guard.assertCanView(Set.of("R9"))).doesNotThrowAnyException();
    }

    @Test
    void nonAdmin_cannotGrantPrivilegedRoles() {
        authenticateAs(Set.of("MANAGER"), Set.of("R1"));

        assertThatThrownBy(() -> guard.assertNoPrivilegeEscalation(Set.of(UserRole.ADMIN)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> guard.assertNoPrivilegeEscalation(Set.of(UserRole.OWNER)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void nonAdmin_canGrantNonPrivilegedRoles() {
        authenticateAs(Set.of("MANAGER"), Set.of("R1"));

        assertThatCode(() -> guard.assertNoPrivilegeEscalation(Set.of(UserRole.SERVER, UserRole.COOK)))
                .doesNotThrowAnyException();
    }

    @Test
    void admin_canGrantPrivilegedRoles() {
        authenticateAs(Set.of("ADMIN"), Set.of());

        assertThatCode(() -> guard.assertNoPrivilegeEscalation(Set.of(UserRole.OWNER)))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAnyRole_whenCallerHasOne_isAllowed() {
        authenticateAs(Set.of("MANAGER"), Set.of("R1"));

        assertThatCode(() -> guard.requireAnyRole(UserRole.ADMIN, UserRole.OWNER, UserRole.MANAGER))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAnyRole_whenCallerHasNone_isDenied() {
        authenticateAs(Set.of("SERVER"), Set.of("R1"));

        assertThatThrownBy(() -> guard.requireAnyRole(UserRole.ADMIN, UserRole.OWNER, UserRole.MANAGER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void missingPrincipal_isDenied() {
        assertThatThrownBy(guard::currentUser).isInstanceOf(AccessDeniedException.class);
    }
}
