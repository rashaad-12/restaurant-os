package com.restaurantos.coresecurity.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import static com.restaurantos.coresecurity.enums.CookieName.ACCESS_TOKEN;
import static io.micrometer.common.util.StringUtils.isNotBlank;
import static java.util.Objects.nonNull;

public class JwtTokenUtil {

    public static String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (isNotBlank(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        Cookie[] cookies = request.getCookies();
        if (nonNull(cookies)) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN.getValue().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

