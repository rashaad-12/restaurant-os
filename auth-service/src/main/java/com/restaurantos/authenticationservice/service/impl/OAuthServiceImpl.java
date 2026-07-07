package com.restaurantos.authenticationservice.service.impl;

import com.restaurantos.authenticationservice.dto.AuthRequest;
import com.restaurantos.authenticationservice.exception.OAuthAuthenticationException;
import com.restaurantos.authenticationservice.oauth.OAuthIdentity;
import com.restaurantos.authenticationservice.oauth.OAuthTokenVerifier;
import com.restaurantos.authenticationservice.oauth.OAuthUserResolver;
import com.restaurantos.authenticationservice.service.OAuthService;
import com.restaurantos.authenticationservice.service.support.AuthTokenIssuer;
import com.restaurantos.coresecurity.enums.Audience;
import com.restaurantos.userservice.model.User;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final OAuthTokenVerifier tokenVerifier;

    private final OAuthUserResolver userResolver;

    private final AuthTokenIssuer tokenIssuer;

    @Override
    public void authenticate(AuthRequest request, Audience audience, HttpServletResponse response) {
        // 1. Verify the provider's ID token — identity comes from the token, not the request.
        OAuthIdentity identity = tokenVerifier.verify(request.getAuthProvider(), request.getIdToken());

        // 2. Resolve (or provision) the platform user for that verified identity.
        User user = userResolver.resolve(identity, audience);

        // 3. Only active accounts may obtain tokens.
        if (!user.isEnabled()) {
            throw new OAuthAuthenticationException("Account is not active");
        }

        // 4. Issue our own access + refresh tokens for the requested audience.
        tokenIssuer.issue(user, audience, response);
    }
}
