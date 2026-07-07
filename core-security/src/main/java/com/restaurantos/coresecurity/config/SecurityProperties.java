package com.restaurantos.coresecurity.config;

import com.restaurantos.coresecurity.enums.Audience;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security.jwt")
public class SecurityProperties {

    private String issuer = "restaurant-os";

    private String publicKey = "classpath:keys/app_public.pem";

    private String privateKey;

    private Duration clockSkew = Duration.ofSeconds(30);

    private final Map<Audience, TokenTtl> ttl = new EnumMap<>(Audience.class);

    private final Cookie cookie = new Cookie();

    private final Cors cors = new Cors();

    public TokenTtl ttlFor(Audience audience) {
        TokenTtl configured = ttl.get(audience);
        if (configured != null) return configured;
        return switch (audience) {
            case STAFF -> new TokenTtl(Duration.ofMinutes(15), Duration.ofDays(1));
            case PARTNER -> new TokenTtl(Duration.ofMinutes(30), Duration.ofDays(7));
            case CUSTOMER -> new TokenTtl(Duration.ofMinutes(30), Duration.ofDays(30));
        };
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenTtl {
        private Duration accessTokenTtl;
        private Duration refreshTokenTtl;
    }

    @Getter
    @Setter
    public static class Cookie {
        private boolean secure = true;
        private String sameSite = "Strict";
        private String path = "/";
        private String domain;
    }

    @Getter
    @Setter
    public static class Cors {
        private boolean enabled = false;
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        private boolean allowCredentials = true;
    }
}
