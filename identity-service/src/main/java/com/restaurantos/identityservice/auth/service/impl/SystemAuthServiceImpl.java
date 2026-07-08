package com.restaurantos.identityservice.auth.service.impl;

import com.restaurantos.coresecurity.config.SecurityProperties;
import com.restaurantos.coresecurity.enums.Audience;
import com.restaurantos.coresecurity.model.TokenRequest;
import com.restaurantos.coresecurity.service.JwtService;
import com.restaurantos.identityservice.auth.dto.ServiceTokenRequest;
import com.restaurantos.identityservice.auth.dto.ServiceTokenResponse;
import com.restaurantos.identityservice.auth.service.SystemAuthService;
import com.restaurantos.identityservice.config.SystemClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Set;

import static com.restaurantos.coresecurity.enums.TokenType.ACCESS;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
@RequiredArgsConstructor
public class SystemAuthServiceImpl implements SystemAuthService {

    private final JwtService jwtService;
    private final SecurityProperties securityProperties;
    private final SystemClientProperties systemClient;

    @Override
    public ServiceTokenResponse issueToken(ServiceTokenRequest request) {
        if (!credentialsMatch(request)) {
            throw new BadCredentialsException("Invalid service client credentials");
        }

        Audience audience = systemClient.getAudience();
        TokenRequest tokenRequest = new TokenRequest(
                systemClient.getClientId(), systemClient.getRoles(), Set.of(), audience);
        String accessToken = jwtService.issue(tokenRequest, ACCESS);

        Duration ttl = securityProperties.ttlFor(audience).getAccessTokenTtl();
        long expiresIn = ttl != null ? ttl.toSeconds() : 0L;

        return ServiceTokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresInSeconds(expiresIn)
                .build();
    }

    private boolean credentialsMatch(ServiceTokenRequest request) {
        String configuredId = systemClient.getClientId();
        String configuredSecret = systemClient.getClientSecret();
        if (isBlank(configuredId) || isBlank(configuredSecret)) {
            return false;
        }
        return configuredId.equals(request.getClientId()) && secretMatches(configuredSecret, request.getClientSecret());
    }

    private boolean secretMatches(String expected, String actual) {
        if (isBlank(actual)) return false;
        // constant-time comparison to avoid leaking secret length/content via timing
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
