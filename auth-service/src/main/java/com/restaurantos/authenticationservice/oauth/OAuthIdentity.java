package com.restaurantos.authenticationservice.oauth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Identity asserted by a verified OIDC ID token. Every field here comes from the
 * cryptographically-verified token — never from the client request.
 */
@Getter
@RequiredArgsConstructor
public class OAuthIdentity {

    private final String provider;

    private final String subject;

    private final String email;

    private final boolean emailVerified;

    private final String name;
}
