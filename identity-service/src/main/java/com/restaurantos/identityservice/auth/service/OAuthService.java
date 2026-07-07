package com.restaurantos.identityservice.auth.service;

import com.restaurantos.identityservice.auth.dto.AuthRequest;
import com.restaurantos.coresecurity.enums.Audience;
import jakarta.servlet.http.HttpServletResponse;

public interface OAuthService {

    void authenticate(AuthRequest request, Audience audience, HttpServletResponse response);
}
