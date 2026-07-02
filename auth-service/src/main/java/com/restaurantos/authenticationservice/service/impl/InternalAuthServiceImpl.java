package com.restaurantos.authenticationservice.service.impl;

import com.restaurantos.authenticationservice.dto.AuthRequest;
import com.restaurantos.authenticationservice.service.InternalAuthService;
import com.restaurantos.authenticationservice.util.CookieUtil;
import com.restaurantos.coresecurity.config.SecurityProperties;
import com.restaurantos.coresecurity.service.JwtService;
import com.restaurantos.userservice.model.User;
import com.restaurantos.userservice.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.restaurantos.coresecurity.enums.AuthType.INTERNAL;

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

        Map<String, Object> claims = CookieUtil.buildClaims(user);
        String accessToken = jwtService.generateToken(INTERNAL, user.getUsername(), claims, true);
        String refreshToken = jwtService.generateToken(INTERNAL, user.getUsername(), claims, false);

        SecurityProperties.JwtProperties config = securityProperties.get(INTERNAL);
        CookieUtil.setAuthCookies(response, accessToken, refreshToken, config.getAccessTokenTtl(), config.getRefreshTokenTtl());
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

        SecurityProperties.JwtProperties config = securityProperties.get(INTERNAL);
        CookieUtil.setAuthCookies(response, newAccessToken, refreshToken, config.getAccessTokenTtl(), config.getRefreshTokenTtl());
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

        SecurityProperties.JwtProperties config = securityProperties.get(INTERNAL);
        CookieUtil.setAuthCookies(response, newAccessToken, newRefreshToken, config.getAccessTokenTtl(), config.getRefreshTokenTtl());
    }

    @Override
    public void revokeToken(HttpServletResponse response) {
        CookieUtil.clearAuthCookies(response);
    }

}
