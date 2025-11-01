package com.restaurantos.coresecurity.service;

import com.restaurantos.coresecurity.enums.AuthType;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public interface JwtService {

    String generateToken(AuthType authType, String user, Map<String, Object> claims, boolean refreshToken);

    boolean isTokenValid(String token, String username);

    Map<String, Object> extractAllClaims(String token);

    String extractUsername(String token);

    Set<String> extractRestaurantCodes(String token);

    Date extractExpiration(String token);

    Set<String> extractRoles(String token);

    Object extractClaim(String token, String name);

}
