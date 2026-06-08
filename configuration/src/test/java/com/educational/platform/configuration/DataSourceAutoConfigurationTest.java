package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the Spring Boot plugin's auto-configuration correctly
 * sets up the H2 in-memory DataSource declared in application.properties.
 * Without the Spring Boot plugin, DataSource auto-configuration would not
 * activate and database-dependent modules (JPA, Liquibase) would fail.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class DataSourceAutoConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Test
    void dataSource_shouldBeAutoConfigured() {
        assertThat(dataSource)
                .as("DataSource must be auto-configured by Spring Boot for JPA and Liquibase")
                .isNotNull();
    }

    @Test
    void dataSource_shouldBeObtainableFromContext() {
        assertThat(applicationContext.getBean(DataSource.class))
                .as("DataSource must be registered as a Spring bean")
                .isNotNull();
    }

    @Test
    void dataSource_shouldProvideValidConnection() {
        assertThatCode(() -> {
            try (Connection conn = dataSource.getConnection()) {
                assertThat(conn.isValid(1))
                        .as("DataSource connection must be valid")
                        .isTrue();
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void dataSource_shouldUseH2InMemoryUrl() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            assertThat(url)
                    .as("DataSource URL must be H2 in-memory as declared in application.properties")
                    .contains("h2")
                    .contains("mem");
        }
    }

    @Test
    void dataSource_shouldUseHikariConnectionPool() {
        assertThat(dataSource.getClass().getName())
                .as("Spring Boot auto-configuration should default to HikariCP connection pool")
                .contains("Hikari");
    }
}
