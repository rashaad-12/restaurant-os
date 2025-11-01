package com.restaurantos.coresecurity.service.impl;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantos.coresecurity.config.SecurityProperties;
import com.restaurantos.coresecurity.enums.AuthType;
import com.restaurantos.coresecurity.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.restaurantos.coresecurity.enums.CookieName.AUTH_TYPE;
import static com.restaurantos.coresecurity.enums.CookieName.RESTAURANT_CODES;
import static com.restaurantos.coresecurity.enums.CookieName.ROLES;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;

@Service
public class JwtServiceImpl implements JwtService {

    @Autowired
    ObjectMapper objectMapper;

    private final Map<AuthType, SecurityProperties.JwtProperties> jwtConfigs;

    public JwtServiceImpl(SecurityProperties props) {
        this.jwtConfigs = Map.of(
                AuthType.INTERNAL, props.getInternal(),
                AuthType.CUSTOMER, props.getCustomer(),
                AuthType.OTP, props.getOtp()
        );
    }

    @Override
    public String generateToken(AuthType authType, String username, Map<String, Object> claims, boolean refreshToken) {
        SecurityProperties.JwtProperties config = jwtConfigs.get(authType);

        long ttl = (refreshToken ? config.getRefreshTokenTtl() : config.getAccessTokenTtl()).toMillis();
        long now = System.currentTimeMillis();

        JwtBuilder jwt = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(now))
                .claim(AUTH_TYPE.getValue(), authType.name())
                .signWith(config.getSigningKey(), SignatureAlgorithm.HS256);

        if (isNotEmpty(claims)) jwt.addClaims(claims);

        if (ttl > 0) jwt.setExpiration(new Date(now + ttl));

        return jwt.compact();
    }

    @Override
    public boolean isTokenValid(String token, String username) {
        try {
            Claims claims = parseClaims(token);
            return nonNull(claims.getSubject()) && claims.getSubject().equals(username) &&
                    nonNull(claims.getExpiration()) && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public Map<String, Object> extractAllClaims(String token) {
        Claims claims = parseClaims(token);
        return new HashMap<>(claims);
    }

    @Override
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public Set<String> extractRestaurantCodes(String token) {
        Claims claims = parseClaims(token);
        Object restaurantCodeObj = claims.get(RESTAURANT_CODES.getValue());
        if (restaurantCodeObj instanceof Collection) {
            return ((Collection<?>) restaurantCodeObj).stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    @Override
    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    @Override
    public Set<String> extractRoles(String token) {
        Claims claims = parseClaims(token);
        Object rolesObj = claims.get(ROLES.getValue());
        if (rolesObj instanceof Collection) {
            return ((Collection<?>) rolesObj).stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    @Override
    public Object extractClaim(String token, String name) {
        Claims claims = parseClaims(token);
        return claims.get(name);
    }

    private Claims parseClaims(String token) {
        try {
            String authTypeName = extractAuthType(token);
            AuthType authType = parseAuthType(authTypeName);

            if (nonNull(authType)) {
                SecurityProperties.JwtProperties config = jwtConfigs.get(authType);
                if (nonNull(config)) {
                    return parseJwtWithKey(token, config.getSigningKey());
                }
            }

            return parseJwtWithAllKeys(token);
        } catch (Exception e) {
            throw new JwtException("Unable to parse JWT", e);
        }
    }

    private String extractAuthType(String token) {
        if (isNull(token)) return null;

        String[] parts = token.split("\\.");
        if (parts.length < 2) return null;

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode node = objectMapper.readTree(decoded);
            JsonNode authTypeNode = node.get(AUTH_TYPE.getValue());
            return (nonNull(authTypeNode) && authTypeNode.isTextual()) ? authTypeNode.asText() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private AuthType parseAuthType(String authTypeName) {
        if (isNull(authTypeName)) return null;

        try {
            return AuthType.valueOf(authTypeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Claims parseJwtWithKey(String token, Key key) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Claims parseJwtWithAllKeys(String token) {
        for (SecurityProperties.JwtProperties config : jwtConfigs.values()) {
            try {
                return parseJwtWithKey(token, config.getSigningKey());
            } catch (JwtException ignored) { }
        }
        throw new JwtException("Unable to parse JWT with any known signing key");
    }

}
