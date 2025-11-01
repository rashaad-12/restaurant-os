package com.restaurantos.authenticationservice.service;

import com.restaurantos.authenticationservice.dto.AuthRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface CustomerAuthService {

    void authenticate(AuthRequest request, HttpServletResponse response);

}
