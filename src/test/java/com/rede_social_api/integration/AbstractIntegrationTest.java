package com.rede_social_api.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final boolean USE_TESTCONTAINERS = isDockerAvailable();

    @BeforeAll
    static void ensureInfrastructure() {
        if (!USE_TESTCONTAINERS) {
            return;
        }
        PostgresRedisContainers.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (USE_TESTCONTAINERS) {
            registry.add("spring.datasource.url", PostgresRedisContainers::getJdbcUrl);
            registry.add("spring.datasource.username", PostgresRedisContainers::getDbUsername);
            registry.add("spring.datasource.password", PostgresRedisContainers::getDbPassword);
            registry.add("spring.data.redis.host", PostgresRedisContainers::getRedisHost);
            registry.add("spring.data.redis.port", PostgresRedisContainers::getRedisPort);
        } else {
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/rede_social");
            registry.add("spring.datasource.username", () -> "rede_social");
            registry.add("spring.datasource.password", () -> "rede_social");
            registry.add("spring.data.redis.host", () -> "localhost");
            registry.add("spring.data.redis.port", () -> "6379");
        }
    }

    private static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
}
