package com.restaurantos.identityservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        private String issuer;

        private String jwksUri;

        private List<String> audiences = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Provisioning {
        private boolean enabled = false;
    }
}
