package com.restaurantos.identityservice.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class OAuthAuthenticationException extends RuntimeException {

    public OAuthAuthenticationException(String message) {
        super(message);
    }

    public OAuthAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
