package com.restaurantos.coresecurity.config;

import com.restaurantos.coresecurity.resolver.CurrentUserArgumentResolver;
import com.restaurantos.coresecurity.resolver.RestaurantCodesArgumentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RestaurantCodesArgumentResolver restaurantCodesArgumentResolver;

    @Autowired
    private CurrentUserArgumentResolver currentUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(restaurantCodesArgumentResolver);
        resolvers.add(currentUserArgumentResolver);
    }
}

