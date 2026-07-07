package com.restaurantos.authenticationservice.service.support;

import com.restaurantos.coresecurity.enums.Audience;
import com.restaurantos.coresecurity.model.TokenRequest;
import com.restaurantos.coresecurity.service.JwtService;
import com.restaurantos.coresecurity.util.AuthCookieManager;
import com.restaurantos.userservice.enums.UserRole;
import com.restaurantos.userservice.model.User;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

import static com.restaurantos.coresecurity.enums.TokenType.ACCESS;
import static com.restaurantos.coresecurity.enums.TokenType.REFRESH;

@Component
@RequiredArgsConstructor
public class AuthTokenIssuer {

    private final JwtService jwtService;

    private final AuthCookieManager cookieManager;

    public void issue(User user, Audience audience, HttpServletResponse response) {
        TokenRequest request = toTokenRequest(user, audience);
        String access = jwtService.issue(request, ACCESS);
        String refresh = jwtService.issue(request, REFRESH);
        cookieManager.setAuthCookies(response, access, refresh, audience);
    }

    public void reissueAccess(User user, Audience audience, String existingRefreshToken, HttpServletResponse response) {
        TokenRequest request = toTokenRequest(user, audience);
        String access = jwtService.issue(request, ACCESS);
        cookieManager.setAuthCookies(response, access, existingRefreshToken, audience);
    }

    private TokenRequest toTokenRequest(User user, Audience audience) {
        Set<String> roles = user.getRoles().stream()
                .map(UserRole::name)
                .collect(Collectors.toSet());
        return new TokenRequest(user.getUsername(), roles, user.getRestaurantCodes(), audience);
    }
}
