package com.restaurantos.coresecurity.service.impl;

import com.restaurantos.coresecurity.config.SecurityProperties;
import com.restaurantos.coresecurity.enums.Audience;
import com.restaurantos.coresecurity.enums.TokenType;
import com.restaurantos.coresecurity.exception.InvalidTokenException;
import com.restaurantos.coresecurity.model.AuthenticatedUser;
import com.restaurantos.coresecurity.model.TokenRequest;
import com.restaurantos.coresecurity.service.JwtService;
import com.restaurantos.coresecurity.util.JwtTokenUtil;
import com.restaurantos.coresecurity.util.KeyLoader;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.restaurantos.coresecurity.enums.CookieName.RESTAURANT_CODES;
import static com.restaurantos.coresecurity.enums.CookieName.ROLES;
import static com.restaurantos.coresecurity.enums.CookieName.TOKEN_TYPE;
import static com.restaurantos.coresecurity.enums.TokenType.REFRESH;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final SecurityProperties properties;

    private java.security.interfaces.RSAPublicKey publicKey;
    private java.security.interfaces.RSAPrivateKey privateKey;
    private JwtParser parser;

    @PostConstruct
    void init() {
        publicKey = KeyLoader.loadPublicKey(properties.getPublicKey());
        if (isNotBlank(properties.getPrivateKey())) {
            privateKey = KeyLoader.loadPrivateKey(properties.getPrivateKey());
        }
        parser = Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(properties.getIssuer())
                .clockSkewSeconds(properties.getClockSkew().toSeconds())
                .build();
        log.info("JwtService initialised (issuer={}, signing={})",
                properties.getIssuer(), nonNull(privateKey) ? "enabled" : "verify-only");
    }

    @Override
    public String issue(TokenRequest request, TokenType tokenType) {
        if (isNull(privateKey)) {
            throw new IllegalStateException("This service is not configured with a signing key and cannot issue tokens");
        }

        SecurityProperties.TokenTtl ttl = properties.ttlFor(request.getAudience());
        Duration lifetime = (tokenType == REFRESH) ? ttl.getRefreshTokenTtl() : ttl.getAccessTokenTtl();
        Instant now = Instant.now();

        JwtBuilder jwt = Jwts.builder()
                .issuer(properties.getIssuer())
                .audience().add(request.getAudience().name()).and()
                .subject(request.getUsername())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .claim(TOKEN_TYPE.getValue(), tokenType.name())
                .claim(ROLES.getValue(), JwtTokenUtil.joinClaim(request.getRoles()))
                .claim(RESTAURANT_CODES.getValue(), JwtTokenUtil.joinClaim(request.getRestaurantCodes()))
                .signWith(privateKey, Jwts.SIG.RS256);

        if (nonNull(lifetime)) {
            jwt.expiration(Date.from(now.plus(lifetime)));
        }

        return jwt.compact();
    }

    @Override
    public AuthenticatedUser verify(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            return new AuthenticatedUser(
                    claims.getSubject(),
                    JwtTokenUtil.splitClaim(asString(claims.get(ROLES.getValue()))),
                    JwtTokenUtil.splitClaim(asString(claims.get(RESTAURANT_CODES.getValue()))),
                    firstAudience(claims.getAudience()),
                    toEnum(TokenType.class, asString(claims.get(TOKEN_TYPE.getValue()))),
                    claims.getId());
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Token verification failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<AuthenticatedUser> tryVerify(String token) {
        try {
            return Optional.of(verify(token));
        } catch (InvalidTokenException e) {
            log.debug("Rejected token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static Audience firstAudience(Set<String> audiences) {
        if (isNull(audiences) || audiences.isEmpty()) return null;
        return toEnum(Audience.class, audiences.iterator().next());
    }

    private static String asString(Object value) {
        return nonNull(value) ? value.toString() : null;
    }

    private static <E extends Enum<E>> E toEnum(Class<E> type, String value) {
        if (isNull(value)) return null;
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
