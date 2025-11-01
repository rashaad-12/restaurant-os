package com.restaurantos.authenticationservice.service;

import com.restaurantos.authenticationservice.dto.AuthRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface OtpAuthService {

    void sendOtp(String username);

    void verifyOtp(AuthRequest request, HttpServletResponse response);

}
