package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the Spring Boot plugin enables Liquibase database migrations
 * during application startup. The configuration module declares liquibase-core
 * as a dependency and application.properties defines the changelog location.
 * These tests verify the changelog is configured and migrations have executed
 * against the H2 in-memory database.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class LiquibaseAutoConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Test
    void liquibaseChangelog_shouldBeConfigured() {
        String changeLog = applicationContext.getEnvironment()
                .getProperty("spring.liquibase.change-log");
        assertThat(changeLog)
                .as("Liquibase changelog path must be configured in application.properties")
                .isNotNull()
                .contains("db.changelog-master");
    }

    @Test
    void liquibaseMigrations_shouldHaveCreatedTables() {
        assertThatCode(() -> {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'")) {
                rs.next();
                int tableCount = rs.getInt(1);
                assertThat(tableCount)
                        .as("Database migrations must create tables in the H2 schema")
                        .isGreaterThan(0);
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void databaseConnection_shouldBeAvailableAfterMigrations() {
        assertThatCode(() -> {
            try (Connection conn = dataSource.getConnection()) {
                assertThat(conn.isValid(1))
                        .as("Database connection must be valid after Liquibase migrations")
                        .isTrue();
            }
        }).doesNotThrowAnyException();
    }
}
