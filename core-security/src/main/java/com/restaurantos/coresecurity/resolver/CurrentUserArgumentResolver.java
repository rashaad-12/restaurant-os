package com.restaurantos.coresecurity.resolver;

import com.restaurantos.coresecurity.annotation.CurrentUser;
import com.restaurantos.coresecurity.model.AuthenticatedUser;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver extends AbstractPrincipalArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        if (AuthenticatedUser.class.isAssignableFrom(parameter.getParameterType())) {
            return currentUser().orElse(null);
        }
        return currentUser().map(AuthenticatedUser::getUsername).orElse(StringUtils.EMPTY);
    }
}
