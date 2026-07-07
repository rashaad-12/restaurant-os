package com.restaurantos.identityservice.auth.oauth;

import com.restaurantos.identityservice.config.OAuthProperties;
import com.restaurantos.identityservice.auth.exception.OAuthAuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthTokenVerifier {

    private final OAuthProperties properties;

    private final Map<String, JwtDecoder> decoders = new ConcurrentHashMap<>();

    public OAuthIdentity verify(String providerName, String idToken) {
        if (isBlank(providerName) || isBlank(idToken)) {
            throw new OAuthAuthenticationException("Missing OAuth provider or ID token");
        }

        String key = providerName.toLowerCase();
        OAuthProperties.Provider provider = properties.getProviders().get(key);
        if (provider == null) {
            throw new OAuthAuthenticationException("Unknown OAuth provider: " + providerName);
        }

        Jwt jwt;
        try {
            jwt = decoders.computeIfAbsent(key, k -> build(provider)).decode(idToken);
        } catch (JwtException e) {
            throw new OAuthAuthenticationException("Invalid ID token: " + e.getMessage(), e);
        }

        return new OAuthIdentity(
                key,
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                readBoolean(jwt.getClaim("email_verified")),
                jwt.getClaimAsString("name"));
    }

    private JwtDecoder build(OAuthProperties.Provider provider) {
        NimbusJwtDecoder decoder = isNotBlank(provider.getJwksUri())
                ? NimbusJwtDecoder.withJwkSetUri(provider.getJwksUri()).build()
                : NimbusJwtDecoder.withIssuerLocation(provider.getIssuer()).build();

        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefaultWithIssuer(provider.getIssuer());

        List<String> audiences = provider.getAudiences().stream()
                .filter(aud -> isNotBlank(aud))
                .map(String::trim)
                .toList();

        if (audiences.isEmpty()) {
            log.warn("OAuth provider '{}' has no audiences configured — ID token 'aud' will NOT be checked",
                    provider.getIssuer());
            decoder.setJwtValidator(defaults);
        } else {
            OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                    JwtClaimNames.AUD,
                    aud -> aud != null && !Collections.disjoint(aud, audiences));
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaults, audienceValidator));
        }
        return decoder;
    }

    private boolean readBoolean(Object value) {
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
