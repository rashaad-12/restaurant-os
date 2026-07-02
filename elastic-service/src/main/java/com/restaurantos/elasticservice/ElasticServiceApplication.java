package com.restaurantos.elasticservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.restaurantos")
public class ElasticServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ElasticServiceApplication.class, args);
    }
}