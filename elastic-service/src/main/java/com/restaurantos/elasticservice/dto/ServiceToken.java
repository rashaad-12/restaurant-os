package com.restaurantos.elasticservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceToken {
    private String accessToken;
    private String tokenType;
    private long expiresInSeconds;
}
