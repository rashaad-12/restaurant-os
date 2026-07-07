package com.restaurantos.authenticationservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Raised when an OAuth login cannot be trusted: unknown provider, invalid/expired
 * ID token, unverified email, unlinked account, or provisioning disabled.
 * Surfaced to the client as HTTP 401.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class OAuthAuthenticationException extends RuntimeException {

    public OAuthAuthenticationException(String message) {
        super(message);
    }

    public OAuthAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
