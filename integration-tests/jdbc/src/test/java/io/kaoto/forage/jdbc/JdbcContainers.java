package io.kaoto.forage.jdbc;

import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton Testcontainers shared by all test classes and runtime suites in this module.
 *
 * <p>The three runtime suites (plain, Quarkus, Spring Boot) run the same test classes within one
 * JVM, so class-scoped {@code @Container} fields restarted each database once per class per suite
 * (Postgres 6 times per run). Containers here start lazily on first use and live until the JVM
 * exits, where Testcontainers' Ryuk reaps them (#434).
 *
 * <p>Because containers survive across classes and suites, tests must not rely on database state
 * being fresh: seed data has to stay read-only and tests that write state must use unique keys per
 * invocation.
 */
final class JdbcContainers {

    static final String POSTGRES_IMAGE_NAME =
            ConfigProvider.getConfig().getValue("postgres.container.image", String.class);
    static final String MYSQL_IMAGE_NAME = ConfigProvider.getConfig().getValue("mysql.container.image", String.class);

    // all init scripts are applied so JdbcTest and MultiTest can share the same instance
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
                    DockerImageName.parse(POSTGRES_IMAGE_NAME).asCompatibleSubstituteFor("postgres"))
            .withExposedPorts(5432)
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("postgresql")
            .withInitScripts("singleTest-postgresql-initScript.sql", "aggregationTest-postgresql-InitScript.sql");

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
                    DockerImageName.parse(MYSQL_IMAGE_NAME).asCompatibleSubstituteFor("mysql"))
            .withExposedPorts(3306)
            .withInitScript("multiITest-mysql-initScript.sql");

    private JdbcContainers() {}

    static synchronized PostgreSQLContainer<?> postgres() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        return POSTGRES;
    }

    static synchronized MySQLContainer<?> mysql() {
        if (!MYSQL.isRunning()) {
            MYSQL.start();
        }
        return MYSQL;
    }
}
