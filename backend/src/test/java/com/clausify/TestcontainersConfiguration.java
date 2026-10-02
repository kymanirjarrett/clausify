package com.clausify;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a throwaway MySQL in Docker for integration tests and points the datasource at it.
 * Import it with {@code @Import(TestcontainersConfiguration.class)}. Spring caches the test context,
 * so test classes with the same configuration share one container per run.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    // Keep in sync with docker-compose.yml (and the Aiven version once pinned, ADR 0004).
    static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(MYSQL_IMAGE);
    }
}
