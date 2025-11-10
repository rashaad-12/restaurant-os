package com.restaurantos.authenticationservice.controller;

import com.restaurantos.authenticationservice.dto.AuthRequest;
import com.restaurantos.authenticationservice.service.InternalAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth-api/v1/auth/internal")
public class InternalAuthController {

    @Autowired
    private InternalAuthService internalAuthService;

    @PostMapping("/login")
    public void login(@RequestBody AuthRequest request, HttpServletResponse response) {
        internalAuthService.authenticate(request, response);
    }

    @PostMapping("/refreshToken")
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        internalAuthService.refreshToken(request, response);
    }

    @PostMapping("/rotateToken")
    public void rotateToken(HttpServletRequest request, HttpServletResponse response) {
        internalAuthService.rotateToken(request, response);
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        internalAuthService.revokeToken(response);
    }

}

