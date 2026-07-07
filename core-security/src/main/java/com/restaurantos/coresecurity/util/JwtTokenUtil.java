package com.restaurantos.coresecurity.util;

import com.restaurantos.coresecurity.enums.CookieName;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static com.restaurantos.coresecurity.enums.CookieName.ACCESS_TOKEN;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class JwtTokenUtil {

    private static final String BEARER_PREFIX = "Bearer ";

    private JwtTokenUtil() {
    }

    public static String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (isNotBlank(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return readCookie(request, ACCESS_TOKEN);
    }

    public static String readCookie(HttpServletRequest request, CookieName name) {
        Cookie[] cookies = request.getCookies();
        if (nonNull(cookies)) {
            for (Cookie cookie : cookies) {
                if (name.getValue().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public static String joinClaim(Collection<String> values) {
        if (isEmpty(values)) return StringUtils.EMPTY;

        return values.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.joining(","));
    }

    public static Set<String> splitClaim(String value) {
        if (isBlank(value)) return Collections.emptySet();

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toUnmodifiableSet());
    }
}
