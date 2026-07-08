package com.restaurantos.identityservice.config;

import com.restaurantos.coresecurity.enums.Audience;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Credentials/claims for the single machine (service-to-service) client that may mint SYSTEM tokens. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security.system-client")
public class SystemClientProperties {

    private String clientId;
    private String clientSecret;
    private Set<String> roles = Set.of("SYSTEM");
    private Audience audience = Audience.PARTNER;
}
