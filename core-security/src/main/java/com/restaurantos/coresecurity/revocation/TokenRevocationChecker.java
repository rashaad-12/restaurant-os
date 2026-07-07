package com.restaurantos.coresecurity.revocation;

@FunctionalInterface
public interface TokenRevocationChecker {
    boolean isRevoked(String tokenId);
}
