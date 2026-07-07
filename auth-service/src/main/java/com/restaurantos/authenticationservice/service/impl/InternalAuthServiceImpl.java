package com.restaurantos.authenticationservice.service.impl;

import com.restaurantos.authenticationservice.dto.AuthRequest;
import com.restaurantos.authenticationservice.service.InternalAuthService;
import com.restaurantos.authenticationservice.service.support.AuthTokenIssuer;
import com.restaurantos.coresecurity.enums.TokenType;
import com.restaurantos.coresecurity.exception.InvalidTokenException;
import com.restaurantos.coresecurity.model.AuthenticatedUser;
import com.restaurantos.coresecurity.service.JwtService;
import com.restaurantos.coresecurity.util.AuthCookieManager;
import com.restaurantos.userservice.model.User;
import com.restaurantos.userservice.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.restaurantos.coresecurity.enums.Audience.STAFF;

@Service
@RequiredArgsConstructor
public class InternalAuthServiceImpl implements InternalAuthService {

    private final UserRepository userRepository;

    private final JwtService jwtService;

    private final AuthCookieManager cookieManager;

    private final AuthTokenIssuer tokenIssuer;

    private final PasswordEncoder encoder;

    @Override
    public void authenticate(AuthRequest request, HttpServletResponse response) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!request.isStaff() || !encoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }

        tokenIssuer.issue(user, STAFF, response);
    }

    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieManager.extractRefreshToken(request);
        User user = loadUserFromRefreshToken(refreshToken);

        tokenIssuer.reissueAccess(user, STAFF, refreshToken, response);
    }

    @Override
    public void rotateToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieManager.extractRefreshToken(request);
        User user = loadUserFromRefreshToken(refreshToken);

        tokenIssuer.issue(user, STAFF, response);
    }

    @Override
    public void revokeToken(HttpServletResponse response) {
        cookieManager.clearAuthCookies(response);
    }

    private User loadUserFromRefreshToken(String refreshToken) {
        AuthenticatedUser principal = jwtService.verify(refreshToken);
        if (principal.getTokenType() != TokenType.REFRESH) {
            throw new InvalidTokenException("A refresh token is required");
        }
        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
