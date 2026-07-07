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

/**
 * Central JWT / security configuration, bound from {@code app.security.jwt.*}.
 *
 * <p>Every value has a production-safe default so a verifying service needs
 * <em>no</em> JWT configuration at all — the public key ships on the classpath
 * inside the core-security jar. Only the issuer (auth-service) sets
 * {@code private-key}; only edge/browser-facing services need {@code cors}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security.jwt")
public class SecurityProperties {

    /** Expected {@code iss} claim; validated on every token. Must match across all services. */
    private String issuer = "restaurant-os";

    /** RSA public key used to verify tokens. Defaults to the key bundled in core-security. */
    private String publicKey = "classpath:keys/app_public.pem";

    /** RSA private key used to sign tokens. Only configured on the issuing service (auth-service). */
    private String privateKey;

    /** Tolerated clock difference between issuer and verifier when checking expiry. */
    private Duration clockSkew = Duration.ofSeconds(30);

    /** Per-audience token lifetimes (only consulted by the issuing service). */
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
        /** Send cookies only over HTTPS. Keep true everywhere except local HTTP dev. */
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
