package com.restaurantos.menuservice;

import com.restaurantos.common.util.DateUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

@SpringBootTest
@Testcontainers
public class AbstractIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeAll
    static void setup() {
        DateUtil.setFixedDateTime(LocalDateTime.of(2025, 11, 6, 10, 30));
    }

    @BeforeEach
    void beforeEach() {
        mongoTemplate.getDb().drop();
    }

    @AfterAll
    static void cleanup() {
        DateUtil.clearFixedDateTime();
    }
}
