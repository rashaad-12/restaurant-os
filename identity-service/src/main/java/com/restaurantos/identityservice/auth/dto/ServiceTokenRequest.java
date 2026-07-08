package com.restaurantos.identityservice.auth.dto;

import lombok.Getter;
import lombok.Setter;

/** Client-credentials request for a service (SYSTEM) token. */
@Getter
@Setter
public class ServiceTokenRequest {

    private String clientId;
    private String clientSecret;
}
