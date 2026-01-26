package com.bhadabazaar.BhadaBazaar;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bhadabazaar_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        registry.add("application.security.jwt.secret-key", () -> "test-secret-key-test-secret-key-test-secret-key");
        registry.add("application.security.jwt.expiration", () -> "3600000");

        registry.add("cloudinary.cloud-name", () -> "test-cloud");
        registry.add("cloudinary.api-key", () -> "test-key");
        registry.add("cloudinary.api-secret", () -> "test-secret");

        registry.add("cloudflare.turnstile.secret-key", () -> "test-secret");
        registry.add("cloudflare.turnstile.url", () -> "https://example.com/turnstile");

        registry.add("twilio.account-sid", () -> "ACxxx-test");
        registry.add("twilio.auth-token", () -> "test-token");
        registry.add("twilio.phone-number", () -> "whatsapp:+1234567890");
    }
}

