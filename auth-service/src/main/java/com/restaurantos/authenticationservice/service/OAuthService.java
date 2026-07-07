package com.restaurantos.authenticationservice.service;

import com.restaurantos.authenticationservice.dto.AuthRequest;
import com.restaurantos.coresecurity.enums.Audience;
import jakarta.servlet.http.HttpServletResponse;

/**
 * OAuth login shared by the customer and partner audiences — same mechanism,
 * different audience. The caller (controller) fixes the {@link Audience}.
 */
public interface OAuthService {

    void authenticate(AuthRequest request, Audience audience, HttpServletResponse response);
}
