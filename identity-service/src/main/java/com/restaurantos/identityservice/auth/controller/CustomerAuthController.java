package com.restaurantos.identityservice.auth.controller;

import com.restaurantos.identityservice.auth.dto.AuthRequest;
import com.restaurantos.identityservice.auth.service.OAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.restaurantos.coresecurity.enums.Audience.CUSTOMER;

@RestController
@RequestMapping("auth-api/v1/auth/customer")
@RequiredArgsConstructor
public class CustomerAuthController {

    private final OAuthService oauthService;

    @PostMapping("/login")
    public void login(@RequestBody AuthRequest request, HttpServletResponse response) {
        oauthService.authenticate(request, CUSTOMER, response);
    }
}
