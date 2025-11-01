package com.restaurantos.coresecurity.config;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Base64;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private JwtProperties internal;
    private JwtProperties customer;
    private JwtProperties otp;

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


