package com.sochi.fieldservice.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    public static PostgreSQLContainer<?> postgres;
    private static boolean useTestcontainers = false;

    static {
        try {
            postgres = new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("fieldservice_test")
                    .withUsername("testuser")
                    .withPassword("testpass");
            postgres.start();
            useTestcontainers = true;
        } catch (Throwable t) {
            System.err.println("Docker environment not available; falling back to local PostgreSQL datasource: " + t.getMessage());
            postgres = null;
            useTestcontainers = false;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (useTestcontainers && postgres != null && postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        }
    }
}
