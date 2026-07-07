package com.restaurantos.coresecurity.config;

import com.restaurantos.coresecurity.resolver.CurrentUserArgumentResolver;
import com.restaurantos.coresecurity.resolver.RestaurantCodesArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RestaurantCodesArgumentResolver restaurantCodesArgumentResolver;

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(restaurantCodesArgumentResolver);
        resolvers.add(currentUserArgumentResolver);
    }
}

