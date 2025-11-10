package com.restaurantos.authenticationservice.util;

import com.restaurantos.coresecurity.util.JwtTokenUtil;
import com.restaurantos.userservice.enums.UserRole;
import com.restaurantos.userservice.model.User;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.restaurantos.coresecurity.enums.CookieName.ACCESS_TOKEN;
import static com.restaurantos.coresecurity.enums.CookieName.REFRESH_TOKEN;
import static com.restaurantos.coresecurity.enums.CookieName.RESTAURANT_CODES;
import static com.restaurantos.coresecurity.enums.CookieName.ROLES;
import static java.util.Objects.isNull;

public class CookieUtil {

    public static String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (isNull(request.getCookies())) {
            throw new JwtException("No cookies present");
        }

        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN.getValue().equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new JwtException("Refresh token not found"));
    }

    public static void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken,
                                      Duration accessTokenTtl, Duration refreshTokenTtl) {
        addCookie(response, ACCESS_TOKEN.getValue(), accessToken, accessTokenTtl);
        addCookie(response, REFRESH_TOKEN.getValue(), refreshToken, refreshTokenTtl);
    }

    public static void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN.getValue(), null, Duration.ZERO);
        addCookie(response, REFRESH_TOKEN.getValue(), null, Duration.ZERO);
    }

    public static Map<String, Object> buildClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(RESTAURANT_CODES.getValue(), JwtTokenUtil.joinClaim(user.getRestaurantCodes()));
        claims.put(ROLES.getValue(), JwtTokenUtil.joinClaim(user.getRoles().stream().map(UserRole::name).toList()));
        return claims;
    }

    public static void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, isNull(value) ? StringUtils.EMPTY : value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

}

