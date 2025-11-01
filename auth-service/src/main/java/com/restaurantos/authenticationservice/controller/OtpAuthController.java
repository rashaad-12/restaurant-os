package com.restaurantos.authenticationservice.controller;

import com.restaurantos.authenticationservice.dto.AuthRequest;
import com.restaurantos.authenticationservice.service.OtpAuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/otp")
public class OtpAuthController {

    @Autowired
    private OtpAuthService otpAuthService;

    @PostMapping("/send")
    public void send(@RequestParam String username) {
        otpAuthService.sendOtp(username);
    }

    @PostMapping("/verify")
    public void verify(@RequestBody AuthRequest request, HttpServletResponse response) {
        otpAuthService.verifyOtp(request, response);
    }
}

