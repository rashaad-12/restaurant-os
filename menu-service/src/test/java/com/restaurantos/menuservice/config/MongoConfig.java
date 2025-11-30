package com.restaurantos.menuservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.time.LocalDateTime;
import java.util.Optional;

@TestConfiguration
@EnableMongoAuditing(dateTimeProviderRef = "fixedDateTimeProvider")
public class MongoConfig {

    @Bean
    public DateTimeProvider fixedDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.of(2025, 11, 6, 10, 30));
    }
}