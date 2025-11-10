package com.restaurantos.coresecurity.resolver;

import com.restaurantos.coresecurity.annotation.CurrentUser;
import com.restaurantos.coresecurity.service.JwtService;
import com.restaurantos.coresecurity.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    private JwtService jwtService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public String resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        if (isNull(request)) throw new IllegalStateException("Invalid request");

        String token = JwtTokenUtil.extractToken(request);

        if (isBlank(token)) return StringUtils.EMPTY;

        String username = jwtService.extractUsername(token);

        if (isBlank(username)) return StringUtils.EMPTY;

        return username;
    }

}
