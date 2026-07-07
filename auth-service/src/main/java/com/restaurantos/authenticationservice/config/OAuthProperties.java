package com.restaurantos.authenticationservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OAuth / OIDC provider registry, bound from {@code app.oauth.*}. Each provider
 * is an OIDC issuer whose ID tokens we verify (Google, Apple, …). Keyed by the
 * provider name the client sends in {@code authProvider} (case-insensitive).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.oauth")
public class OAuthProperties {

    private final Map<String, Provider> providers = new HashMap<>();

    private final Provisioning provisioning = new Provisioning();

    @Getter
    @Setter
    public static class Provider {
        /** OIDC issuer, e.g. https://accounts.google.com — used for discovery + iss validation. */
        private String issuer;

        /** Optional explicit JWKS URI; if blank it is discovered from the issuer. */
        private String jwksUri;

        /** Accepted {@code aud} values (our registered client IDs: web/ios/android). */
        private List<String> audiences = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Provisioning {
        /** Just-in-time create a user on first successful OAuth login. Off by default. */
        private boolean enabled = false;
    }
}
