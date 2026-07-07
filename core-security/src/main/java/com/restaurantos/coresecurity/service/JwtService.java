package com.restaurantos.coresecurity.service;

import com.restaurantos.coresecurity.enums.TokenType;
import com.restaurantos.coresecurity.exception.InvalidTokenException;
import com.restaurantos.coresecurity.model.AuthenticatedUser;
import com.restaurantos.coresecurity.model.TokenRequest;

import java.util.Optional;


public interface JwtService {

    String issue(TokenRequest request, TokenType tokenType);

    AuthenticatedUser verify(String token);

    Optional<AuthenticatedUser> tryVerify(String token);
}
