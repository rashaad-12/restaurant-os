package com.restaurantos.identityservice.auth.oauth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OAuthIdentity {

    private final String provider;

    private final String subject;

    private final String email;

    private final boolean emailVerified;

    private final String name;
}
