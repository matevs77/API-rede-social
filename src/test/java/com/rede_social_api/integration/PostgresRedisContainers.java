package com.rede_social_api.integration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

final class PostgresRedisContainers {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("rede_social")
            .withUsername("rede_social")
            .withPassword("rede_social");

    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static boolean started;

    private PostgresRedisContainers() {}

    static void start() {
        if (!started) {
            POSTGRES.start();
            REDIS.start();
            started = true;
        }
    }

    static String getJdbcUrl() {
        return POSTGRES.getJdbcUrl();
    }

    static String getDbUsername() {
        return POSTGRES.getUsername();
    }

    static String getDbPassword() {
        return POSTGRES.getPassword();
    }

    static String getRedisHost() {
        return REDIS.getHost();
    }

    static String getRedisPort() {
        return REDIS.getMappedPort(6379).toString();
    }
}
