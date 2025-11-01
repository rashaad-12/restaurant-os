package com.restaurantos.authenticationservice.service.impl;

import com.restaurantos.authenticationservice.dto.AuthRequest;
import com.restaurantos.authenticationservice.service.InternalAuthService;
import com.restaurantos.coresecurity.config.SecurityProperties;
import com.restaurantos.coresecurity.service.JwtService;
import com.restaurantos.userservice.model.Restaurant;
import com.restaurantos.userservice.model.User;
import com.restaurantos.userservice.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @Override
    public void authenticate(AuthRequest request, HttpServletResponse response) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!request.isInternal() || !encoder.matches(request.getPassword(), user.getPassword())) {
                throw new BadCredentialsException("Bad credentials");
        }

        Map<String, Object> claims = new HashMap<>();

        Set<String> restaurantCodes = user.getRestaurants().stream()
                .map(Restaurant::getCode)
                .collect(Collectors.toSet());

        claims.put(RESTAURANT_CODES.getValue(), String.join(",", restaurantCodes));
        
        String accessToken = jwtService.generateToken(INTERNAL, user.getUsername(), claims, true);
        String refreshToken = jwtService.generateToken(INTERNAL, user.getUsername(), claims, false);
        setAuthCookies(response, accessToken, refreshToken);
    }

    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);

        String username = jwtService.extractUsername(refreshToken);
        if (!jwtService.isTokenValid(refreshToken, username)) {
            throw new JwtException("Invalid refresh token");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Map<String, Object> claims = new HashMap<>();

        Set<String> restaurantCodes = user.getRestaurants().stream()
                .map(Restaurant::getCode)
                .collect(Collectors.toSet());

        claims.put(RESTAURANT_CODES.getValue(), String.join(",", restaurantCodes));


        String newAccessToken = jwtService.generateToken(INTERNAL, username, claims, true);
        setAuthCookies(response, newAccessToken, refreshToken);
    }

    @Override
    public void rotateToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);

        String username = jwtService.extractUsername(refreshToken);
        if (!jwtService.isTokenValid(refreshToken, username)) {
            throw new JwtException("Invalid refresh token");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Map<String, Object> claims = new HashMap<>();

        Set<String> restaurantCodes = user.getRestaurants().stream()
                .map(Restaurant::getCode)
                .collect(Collectors.toSet());

        claims.put(RESTAURANT_CODES.getValue(), String.join(",", restaurantCodes));

        String newAccessToken = jwtService.generateToken(INTERNAL, username, claims, true);
        String newRefreshToken = jwtService.generateToken(INTERNAL, username, claims, false);
        setAuthCookies(response, newAccessToken, newRefreshToken);
    }

    @Override
    public void revokeToken(HttpServletResponse response) {
        clearAuthCookies(response);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (isNull(request.getCookies())) {
            throw new IllegalStateException("No cookies present");
        }

        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN.getValue().equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Refresh token not found"));
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        Duration accessTokenTtl = securityProperties.getInternal().getAccessTokenTtl();
        Duration refreshTokenTtl = securityProperties.getInternal().getRefreshTokenTtl();
        addCookie(response, ACCESS_TOKEN.getValue(), accessToken, accessTokenTtl);
        addCookie(response, REFRESH_TOKEN.getValue(), refreshToken, refreshTokenTtl);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN.getValue(), null, Duration.ZERO);
        addCookie(response, REFRESH_TOKEN.getValue(), null, Duration.ZERO);
    }

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, isNull(value) ? "" : value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

}
