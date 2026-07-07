package com.restaurantos.coresecurity.authz;

import com.restaurantos.coresecurity.enums.Audience;
import com.restaurantos.coresecurity.enums.TokenType;
import com.restaurantos.coresecurity.model.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScopeGuardTest {

    private final ScopeGuard guard = new ScopeGuard();

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
    void withinScope_subset_isAllowed() {
        authenticateAs(Set.of("MANAGER"), Set.of("R1", "R2"));

        assertThatCode(() -> guard.assertWithinScope(Set.of("R1"))).doesNotThrowAnyException();
    }

    @Test
    void withinScope_notSubset_isDenied() {
        authenticateAs(Set.of("MANAGER"), Set.of("R1"));

        assertThatThrownBy(() -> guard.assertWithinScope(Set.of("R1", "R2")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void withinScope_emptyRequest_isDeniedForNonAdmin() {
        authenticateAs(Set.of("MANAGER"), Set.of("R1"));

        assertThatThrownBy(() -> guard.assertWithinScope(Set.of()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void canView_intersecting_isAllowed() {
        authenticateAs(Set.of("SERVER"), Set.of("R1", "R2"));

        assertThatCode(() -> guard.assertCanView(Set.of("R2", "R3"))).doesNotThrowAnyException();
    }

    @Test
    void canView_disjoint_isDenied() {
        authenticateAs(Set.of("SERVER"), Set.of("R1"));

        assertThatThrownBy(() -> guard.assertCanView(Set.of("R2")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void admin_bypassesScopeAndView() {
        authenticateAs(Set.of("ADMIN"), Set.of());

        assertThatCode(() -> guard.assertWithinScope(Set.of("R9"))).doesNotThrowAnyException();
        assertThatCode(() -> guard.assertCanView(Set.of("R9"))).doesNotThrowAnyException();
        assertThat(guard.isPlatformAdmin()).isTrue();
    }

    @Test
    void hasRestaurantScope_reflectsCodes() {
        authenticateAs(Set.of("CUSTOMER"), Set.of());
        assertThat(guard.hasRestaurantScope()).isFalse();

        SecurityContextHolder.clearContext();
        authenticateAs(Set.of("SERVER"), Set.of("R1"));
        assertThat(guard.hasRestaurantScope()).isTrue();
    }

    @Test
    void missingPrincipal_isDenied() {
        assertThatThrownBy(guard::currentUser).isInstanceOf(AccessDeniedException.class);
    }
}
