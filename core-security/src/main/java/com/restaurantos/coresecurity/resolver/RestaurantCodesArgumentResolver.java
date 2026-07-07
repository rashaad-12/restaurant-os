package com.restaurantos.coresecurity.resolver;

import com.restaurantos.coresecurity.annotation.RestaurantCodes;
import com.restaurantos.coresecurity.model.AuthenticatedUser;
import lombok.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Collections;

@Component
public class RestaurantCodesArgumentResolver extends AbstractPrincipalArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RestaurantCodes.class);
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return currentUser()
                .map(AuthenticatedUser::getRestaurantCodes)
                .orElseGet(Collections::emptySet);
    }
}
