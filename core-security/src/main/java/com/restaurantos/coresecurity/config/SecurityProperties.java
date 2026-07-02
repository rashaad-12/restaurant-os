package com.restaurantos.coresecurity.config;

import com.restaurantos.coresecurity.enums.AuthType;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

import static java.util.Objects.isNull;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security.jwt")
public class SecurityProperties {

    private final Map<AuthType, JwtProperties> configs = new EnumMap<>(AuthType.class);

    public JwtProperties get(AuthType type) {
        JwtProperties properties = configs.get(type);

        if (isNull(properties))
            throw new IllegalStateException("JWT config does not exist for " + type);

        return properties;
    }

    @Getter
    @Setter
    public static class JwtProperties {
        private String secret;
        private Duration accessTokenTtl;
        private Duration refreshTokenTtl;

        public Key getSigningKey() {
            return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        }
    }
}