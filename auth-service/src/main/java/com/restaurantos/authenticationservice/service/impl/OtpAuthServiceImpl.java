package com.restaurantos.authenticationservice.service.impl;

import com.restaurantos.authenticationservice.dto.AuthRequest;
import com.restaurantos.authenticationservice.service.OtpAuthService;
import com.restaurantos.coresecurity.config.SecurityProperties;
import com.restaurantos.coresecurity.service.JwtService;
import com.restaurantos.userservice.model.User;
import com.restaurantos.userservice.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.restaurantos.coresecurity.enums.AuthType.OTP;
import static com.restaurantos.coresecurity.enums.CookieName.ACCESS_TOKEN;
import static com.restaurantos.coresecurity.enums.CookieName.REFRESH_TOKEN;

@Service
public class OtpAuthServiceImpl implements OtpAuthService {

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Override
    public void sendOtp(String username) {
        // TODO: enable messaging to number
    }

    @Override
    public void verifyOtp(AuthRequest request, HttpServletResponse response) {

        // TODO: need to verify otp from db
        boolean isValidOtp = true;

        if (!isValidOtp) {
            throw new BadCredentialsException("Invalid OTP");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String accessToken = jwtService.generateToken(OTP, user.getUsername(), null, true);
        String refreshToken = jwtService.generateToken(OTP, user.getUsername(), null, false);

        Duration accessTokenTtl = securityProperties.getOtp().getAccessTokenTtl();
        Duration refreshTokenTtl = securityProperties.getOtp().getRefreshTokenTtl();

        addCookie(response, ACCESS_TOKEN.getValue(), accessToken, accessTokenTtl);
        addCookie(response, REFRESH_TOKEN.getValue(), refreshToken, refreshTokenTtl);
    }

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value == null ? "" : value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}




