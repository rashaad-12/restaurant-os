package com.restaurantos.coresecurity.config;

import com.restaurantos.coresecurity.revocation.TokenRevocationChecker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenRevocationConfig {

    @Bean
    @ConditionalOnMissingBean
    public TokenRevocationChecker tokenRevocationChecker() {
        return tokenId -> false;
    }
}
