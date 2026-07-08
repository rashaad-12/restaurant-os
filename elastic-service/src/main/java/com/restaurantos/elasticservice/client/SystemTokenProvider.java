package com.restaurantos.elasticservice.client;

import com.restaurantos.elasticservice.dto.ServiceToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemTokenProvider {

    private static final Duration REFRESH_SKEW = Duration.ofSeconds(60);
    private static final long FALLBACK_TTL_SECONDS = 300;

    private final RestClient identityServiceRestClient;

    @Value("${integration.identity-service.client-id}")
    private String clientId;

    @Value("${integration.identity-service.client-secret}")
    private String clientSecret;

    private volatile String cachedToken;
    private volatile Instant refreshAt = Instant.EPOCH;

    public String token() {
        if (Instant.now().isAfter(refreshAt)) {
            synchronized (this) {
                if (Instant.now().isAfter(refreshAt)) {
                    fetch();
                }
            }
        }
        return cachedToken;
    }

    private void fetch() {
        ServiceToken response = identityServiceRestClient.post()
                .uri("/auth-api/v1/auth/system/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("clientId", clientId, "clientSecret", clientSecret))
                .retrieve()
                .body(ServiceToken.class);

        if (isNull(response) || isBlank(response.getAccessToken())) {
            throw new IllegalStateException("identity-service returned no system token");
        }

        cachedToken = response.getAccessToken();
        long ttl = response.getExpiresInSeconds() > 0 ? response.getExpiresInSeconds() : FALLBACK_TTL_SECONDS;
        refreshAt = Instant.now().plusSeconds(ttl).minus(REFRESH_SKEW);
        log.debug("Fetched SYSTEM token from identity-service (ttl={}s)", ttl);
    }
}
