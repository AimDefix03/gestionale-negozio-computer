package it.giovannidefilippo.gestionale.common;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

public abstract class PostgreSqlIntegrationTestSupport {
    private static final boolean ENABLED = Boolean.getBoolean("gestionale.postgresql.it.enabled");
    private static final PostgreSQLContainer POSTGRESQL = ENABLED ? startContainer() : null;

    private static PostgreSQLContainer startContainer() {
        PostgreSQLContainer container = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("gestionale_postgresql_it")
                .withUsername("gestionale_postgresql_it")
                .withPassword("tc-" + UUID.randomUUID())
                .withLabel("it.giovannidefilippo.gestionale.postgresql-it", "true");
        container.start();
        return container;
    }

    @DynamicPropertySource
    static void configurePostgreSql(DynamicPropertyRegistry registry) {
        if (!ENABLED) {
            return;
        }
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRESQL::getUsername);
        registry.add("spring.flyway.password", POSTGRESQL::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}
