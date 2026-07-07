package com.restaurantos.coresecurity.util;

import com.restaurantos.coresecurity.config.SecurityProperties;
import com.restaurantos.coresecurity.enums.Audience;
import com.restaurantos.coresecurity.exception.InvalidTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.restaurantos.coresecurity.enums.CookieName.ACCESS_TOKEN;
import static com.restaurantos.coresecurity.enums.CookieName.REFRESH_TOKEN;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
@RequiredArgsConstructor
public class AuthCookieManager {

    private final SecurityProperties properties;

    public void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken, Audience audience) {
        SecurityProperties.TokenTtl ttl = properties.ttlFor(audience);
        addCookie(response, ACCESS_TOKEN.getValue(), accessToken, ttl.getAccessTokenTtl());
        addCookie(response, REFRESH_TOKEN.getValue(), refreshToken, ttl.getRefreshTokenTtl());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN.getValue(), null, Duration.ZERO);
        addCookie(response, REFRESH_TOKEN.getValue(), null, Duration.ZERO);
    }

    public String extractRefreshToken(HttpServletRequest request) {
        String token = JwtTokenUtil.readCookie(request, REFRESH_TOKEN);
        if (isBlank(token)) {
            throw new InvalidTokenException("Refresh token cookie not present");
        }
        return token;
    }

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
        SecurityProperties.Cookie config = properties.getCookie();

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, isNull(value) ? StringUtils.EMPTY : value)
                .httpOnly(true)
                .secure(config.isSecure())
                .sameSite(config.getSameSite())
                .path(config.getPath());

        if (nonNull(config.getDomain())) builder.domain(config.getDomain());
        if (nonNull(maxAge)) builder.maxAge(maxAge);

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }
}
