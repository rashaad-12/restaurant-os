package com.restaurantos.authenticationservice.service;

import com.restaurantos.authenticationservice.dto.AuthRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface InternalAuthService {

    void authenticate(AuthRequest request, HttpServletResponse response);

    void refreshToken(HttpServletRequest request, HttpServletResponse response);

    void rotateToken(HttpServletRequest request, HttpServletResponse response);

    void revokeToken(HttpServletResponse response);
}
