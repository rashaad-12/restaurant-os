package com.restaurantos.identityservice.auth.dto;

import lombok.Builder;
import lombok.Getter;

/** Service (SYSTEM) access token issued to a machine client. */
@Getter
@Builder
public class ServiceTokenResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresInSeconds;
}
