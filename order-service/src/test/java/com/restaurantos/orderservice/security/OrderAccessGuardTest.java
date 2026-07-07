package com.restaurantos.orderservice.security;

import com.restaurantos.coresecurity.authz.ScopeGuard;
import com.restaurantos.coresecurity.enums.Audience;
import com.restaurantos.coresecurity.enums.TokenType;
import com.restaurantos.coresecurity.model.AuthenticatedUser;
import com.restaurantos.orderservice.model.Order;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderAccessGuardTest {

    private final OrderAccessGuard guard = new OrderAccessGuard(new ScopeGuard());

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String username, Set<String> roles, Set<String> restaurantCodes) {
        AuthenticatedUser principal = new AuthenticatedUser(
                username, roles, restaurantCodes, Audience.STAFF, TokenType.ACCESS, "tid");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, Set.of()));
    }

    private Order order(String customerId, String restaurantCode) {
        return Order.builder().customerId(customerId).restaurantCode(restaurantCode).build();
    }

    @Test
    void owner_canViewOwnOrder() {
        authenticateAs("cust@example.com", Set.of("CUSTOMER"), Set.of());

        assertThatCode(() -> guard.assertCanView(order("cust@example.com", "R1"))).doesNotThrowAnyException();
    }

    @Test
    void customer_cannotViewOthersOrder() {
        authenticateAs("cust@example.com", Set.of("CUSTOMER"), Set.of());

        assertThatThrownBy(() -> guard.assertCanView(order("someone-else", "R1")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void staffInScope_canViewOrder() {
        authenticateAs("server@example.com", Set.of("SERVER"), Set.of("R1"));

        assertThatCode(() -> guard.assertCanView(order("cust@example.com", "R1"))).doesNotThrowAnyException();
    }

    @Test
    void staffOutOfScope_cannotViewOrder() {
        authenticateAs("server@example.com", Set.of("SERVER"), Set.of("R2"));

        assertThatThrownBy(() -> guard.assertCanView(order("cust@example.com", "R1")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assertStaffScope_rejectsTheOwningCustomer() {
        authenticateAs("cust@example.com", Set.of("CUSTOMER"), Set.of());

        assertThatThrownBy(() -> guard.assertStaffScope(order("cust@example.com", "R1")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assertOwnerOrStaffScope_allowsOwnerAndStaff_deniesOthers() {
        authenticateAs("cust@example.com", Set.of("CUSTOMER"), Set.of());
        assertThatCode(() -> guard.assertOwnerOrStaffScope(order("cust@example.com", "R1"))).doesNotThrowAnyException();

        SecurityContextHolder.clearContext();
        authenticateAs("mgr@example.com", Set.of("MANAGER"), Set.of("R1"));
        assertThatCode(() -> guard.assertOwnerOrStaffScope(order("cust@example.com", "R1"))).doesNotThrowAnyException();

        SecurityContextHolder.clearContext();
        authenticateAs("other@example.com", Set.of("CUSTOMER"), Set.of());
        assertThatThrownBy(() -> guard.assertOwnerOrStaffScope(order("cust@example.com", "R1")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void actingAsStaff_trueForScopedAndAdmin_falseForCustomer() {
        authenticateAs("server@example.com", Set.of("SERVER"), Set.of("R1"));
        assertThat(guard.actingAsStaff()).isTrue();

        SecurityContextHolder.clearContext();
        authenticateAs("admin@example.com", Set.of("ADMIN"), Set.of());
        assertThat(guard.actingAsStaff()).isTrue();

        SecurityContextHolder.clearContext();
        authenticateAs("cust@example.com", Set.of("CUSTOMER"), Set.of());
        assertThat(guard.actingAsStaff()).isFalse();
    }
}
