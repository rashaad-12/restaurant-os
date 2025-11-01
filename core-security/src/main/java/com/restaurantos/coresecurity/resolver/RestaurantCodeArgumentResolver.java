package com.restaurantos.coresecurity.resolver;

import com.restaurantos.coresecurity.annotation.RestaurantCodes;
import com.restaurantos.coresecurity.service.JwtService;
import com.restaurantos.coresecurity.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static com.restaurantos.coresecurity.enums.CookieName.RESTAURANT_CODES;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class RestaurantCodeArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    private JwtService jwtService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RestaurantCodes.class);
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        if (isNull(request)) throw new IllegalStateException("Invalid request");

        String token = JwtTokenUtil.extractToken(request);

        if (isBlank(token)) return Collections.emptySet();

        Object restaurantCodesClaim = jwtService.extractClaim(token, RESTAURANT_CODES.getValue());
        String restaurantCodes = restaurantCodesClaim.toString();

        if (isBlank(restaurantCodes)) return Collections.emptySet();

        return Arrays.stream(restaurantCodes.split(","))
                .collect(Collectors.toSet());
    }
}
