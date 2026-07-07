package com.restaurantos.orderservice.security;

import com.restaurantos.coresecurity.authz.ScopeGuard;
import com.restaurantos.orderservice.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class OrderAccessGuard {

    private final ScopeGuard scopeGuard;

    public String callerUsername() {
        return scopeGuard.currentUser().getUsername();
    }

    public boolean actingAsStaff() {
        return scopeGuard.isPlatformAdmin() || scopeGuard.hasRestaurantScope();
    }

    public boolean isPlatformAdmin() {
        return scopeGuard.isPlatformAdmin();
    }

    public boolean isOwner(Order order) {
        return order.getCustomerId() != null && order.getCustomerId().equals(callerUsername());
    }

    public void assertCanView(Order order) {
        if (isOwner(order)) {
            return;
        }
        scopeGuard.assertCanView(Set.of(order.getRestaurantCode()));
    }

    public void assertStaffScope(Order order) {
        scopeGuard.assertWithinScope(Set.of(order.getRestaurantCode()));
    }

    public void assertOwnerOrStaffScope(Order order) {
        if (isOwner(order)) {
            return;
        }
        scopeGuard.assertWithinScope(Set.of(order.getRestaurantCode()));
    }
}
