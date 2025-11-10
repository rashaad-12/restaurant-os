package com.restaurantos.authenticationservice.service.impl;

import com.restaurantos.authenticationservice.dto.AuthRequest;
import com.restaurantos.authenticationservice.service.OtpAuthService;
import com.restaurantos.authenticationservice.util.CookieUtil;
import com.restaurantos.coresecurity.config.SecurityProperties;
import com.restaurantos.coresecurity.service.JwtService;
import com.restaurantos.userservice.model.User;
import com.restaurantos.userservice.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

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

    private Duration accessTokenTtl;

    private Duration refreshTokenTtl;

    @PostConstruct
    public void init() {
        accessTokenTtl = securityProperties.getOtp().getAccessTokenTtl();
        refreshTokenTtl = securityProperties.getOtp().getRefreshTokenTtl();
    }

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

        Map<String, Object> claims = CookieUtil.buildClaims(user);
        String accessToken = jwtService.generateToken(OTP, user.getUsername(), claims, true);
        String refreshToken = jwtService.generateToken(OTP, user.getUsername(), claims, false);
        CookieUtil.setAuthCookies(response, accessToken, refreshToken, accessTokenTtl, refreshTokenTtl);
    }
}




