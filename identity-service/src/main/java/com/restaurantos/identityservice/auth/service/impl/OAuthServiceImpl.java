package com.restaurantos.identityservice.auth.service.impl;

import com.restaurantos.identityservice.auth.dto.AuthRequest;
import com.restaurantos.identityservice.auth.exception.OAuthAuthenticationException;
import com.restaurantos.identityservice.auth.oauth.OAuthIdentity;
import com.restaurantos.identityservice.auth.oauth.OAuthTokenVerifier;
import com.restaurantos.identityservice.auth.oauth.OAuthUserResolver;
import com.restaurantos.identityservice.auth.service.OAuthService;
import com.restaurantos.identityservice.auth.service.support.AuthTokenIssuer;
import com.restaurantos.coresecurity.enums.Audience;
import com.restaurantos.identityservice.user.model.User;
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
        OAuthIdentity identity = tokenVerifier.verify(request.getAuthProvider(), request.getIdToken());

        User user = userResolver.resolve(identity, audience);

        if (!user.isEnabled()) {
            throw new OAuthAuthenticationException("Account is not active");
        }

        tokenIssuer.issue(user, audience, response);
    }
}
