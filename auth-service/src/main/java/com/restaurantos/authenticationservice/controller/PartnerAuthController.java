package com.restaurantos.authenticationservice.controller;

import com.restaurantos.authenticationservice.dto.AuthRequest;
import com.restaurantos.authenticationservice.service.OAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.restaurantos.coresecurity.enums.Audience.PARTNER;

@RestController
@RequestMapping("auth-api/v1/auth/partner")
@RequiredArgsConstructor
public class PartnerAuthController {

    private final OAuthService oauthService;

    @PostMapping("/login")
    public void login(@RequestBody AuthRequest request, HttpServletResponse response) {
        oauthService.authenticate(request, PARTNER, response);
    }
}
