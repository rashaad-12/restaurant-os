package com.restaurantos.authenticationservice.service.impl;

import com.restaurantos.authenticationservice.dto.AuthRequest;
import com.restaurantos.authenticationservice.service.InternalAuthService;
import com.restaurantos.authenticationservice.util.CookieUtil;
import com.restaurantos.coresecurity.config.SecurityProperties;
import com.restaurantos.coresecurity.service.JwtService;
import com.restaurantos.coresecurity.util.JwtTokenUtil;
import com.restaurantos.userservice.enums.UserRole;
import com.restaurantos.userservice.model.Restaurant;
import com.restaurantos.userservice.model.User;
import com.restaurantos.userservice.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.restaurantos.coresecurity.enums.CookieName.ROLES;
import static java.util.Objects.isNull;
import static com.restaurantos.coresecurity.enums.AuthType.INTERNAL;
import static com.restaurantos.coresecurity.enums.CookieName.RESTAURANT_CODES;
import static com.restaurantos.coresecurity.enums.CookieName.ACCESS_TOKEN;
import static com.restaurantos.coresecurity.enums.CookieName.REFRESH_TOKEN;

@Service
public class InternalAuthServiceImpl implements InternalAuthService {

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder encoder;

    private Duration accessTokenTtl;

    private Duration refreshTokenTtl;

    @PostConstruct
    public void init() {
        accessTokenTtl = securityProperties.getInternal().getAccessTokenTtl();
        refreshTokenTtl = securityProperties.getInternal().getRefreshTokenTtl();
    }

    @Override
    public void authenticate(AuthRequest request, HttpServletResponse response) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!request.isInternal() || !encoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }

        Map<String, Object> claims = CookieUtil.buildClaims(user);
        String accessToken = jwtService.generateToken(INTERNAL, user.getUsername(), claims, true);
        String refreshToken = jwtService.generateToken(INTERNAL, user.getUsername(), claims, false);
        CookieUtil.setAuthCookies(response, accessToken, refreshToken, accessTokenTtl, refreshTokenTtl);
    }

    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = CookieUtil.extractRefreshTokenFromCookie(request);

        String username = jwtService.extractUsername(refreshToken);
        if (!jwtService.isTokenValid(refreshToken, username)) {
            throw new JwtException("Invalid refresh token");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Map<String, Object> claims = CookieUtil.buildClaims(user);
        String newAccessToken = jwtService.generateToken(INTERNAL, username, claims, true);
        CookieUtil.setAuthCookies(response, newAccessToken, refreshToken, accessTokenTtl, refreshTokenTtl);
    }

    @Override
    public void rotateToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = CookieUtil.extractRefreshTokenFromCookie(request);

        String username = jwtService.extractUsername(refreshToken);
        if (!jwtService.isTokenValid(refreshToken, username)) {
            throw new JwtException("Invalid refresh token");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Map<String, Object> claims = CookieUtil.buildClaims(user);
        String newAccessToken = jwtService.generateToken(INTERNAL, username, claims, true);
        String newRefreshToken = jwtService.generateToken(INTERNAL, username, claims, false);
        CookieUtil.setAuthCookies(response, newAccessToken, newRefreshToken, accessTokenTtl, refreshTokenTtl);
    }

    @Override
    public void revokeToken(HttpServletResponse response) {
        CookieUtil.clearAuthCookies(response);
    }

}
