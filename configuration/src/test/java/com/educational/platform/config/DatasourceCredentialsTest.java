package com.educational.platform.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mock.env.MockEnvironment;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class DatasourceCredentialsTest {

    private static final String USERNAME_PLACEHOLDER = "${SPRING_DATASOURCE_USERNAME:sa}";
    private static final String PASSWORD_PLACEHOLDER = "${SPRING_DATASOURCE_PASSWORD:}";
    private static final String H2_CONSOLE_PLACEHOLDER = "${SPRING_H2_CONSOLE_ENABLED:false}";

    @Test
    void applicationProperties_mustNotContainLiteralDatasourcePassword() throws IOException {
        // when
        final Properties properties = loadApplicationProperties();

        // then
        assertThat(properties.getProperty("spring.datasource.password")).isEqualTo(PASSWORD_PLACEHOLDER);
    }

    @Test
    void applicationProperties_mustNotContainLiteralDatasourceUsername() throws IOException {
        // when
        final Properties properties = loadApplicationProperties();

        // then
        assertThat(properties.getProperty("spring.datasource.username")).isEqualTo(USERNAME_PLACEHOLDER);
    }

    @Test
    void applicationProperties_mustNotEnableH2ConsoleUnconditionally() throws IOException {
        // when
        final Properties properties = loadApplicationProperties();

        // then
        assertThat(properties.getProperty("spring.h2.console.enabled")).isEqualTo(H2_CONSOLE_PLACEHOLDER);
    }

    @Test
    void placeholders_fallBackToDefaultsWhenEnvironmentVariablesAreAbsent() throws IOException {
        // given
        final MockEnvironment environment = environmentWithApplicationProperties(Map.of());

        // when
        final String username = environment.getProperty("spring.datasource.username");
        final String password = environment.getProperty("spring.datasource.password");
        final Boolean h2ConsoleEnabled = environment.getProperty("spring.h2.console.enabled", Boolean.class);

        // then
        assertThat(username).isEqualTo("sa");
        assertThat(password).isEmpty();
        assertThat(h2ConsoleEnabled).isFalse();
    }

    @Test
    void placeholders_resolveFromEnvironmentVariablesWhenPresent() throws IOException {
        // given
        final MockEnvironment environment = environmentWithApplicationProperties(Map.of(
                "SPRING_DATASOURCE_USERNAME", "platform",
                "SPRING_DATASOURCE_PASSWORD", "s3cr3t",
                "SPRING_H2_CONSOLE_ENABLED", "true"));

        // when
        final String username = environment.getProperty("spring.datasource.username");
        final String password = environment.getProperty("spring.datasource.password");
        final Boolean h2ConsoleEnabled = environment.getProperty("spring.h2.console.enabled", Boolean.class);

        // then
        assertThat(username).isEqualTo("platform");
        assertThat(password).isEqualTo("s3cr3t");
        assertThat(h2ConsoleEnabled).isTrue();
    }

    @Test
    void placeholders_resolveEmptyPasswordWhenEnvironmentVariableIsBlank() throws IOException {
        // given
        final MockEnvironment environment = environmentWithApplicationProperties(Map.of("SPRING_DATASOURCE_PASSWORD", ""));

        // when
        final String password = environment.getProperty("spring.datasource.password");

        // then
        assertThat(password).isEmpty();
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    class WhenCredentialsAreNotProvided {

        @Autowired
        private Environment environment;

        @Autowired
        private DataSource dataSource;

        @Test
        void datasourceCredentials_fallBackToDefaults() {
            assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("sa");
            assertThat(environment.getProperty("spring.datasource.password")).isEmpty();
        }

        @Test
        void h2Console_isDisabledByDefault() {
            assertThat(environment.getProperty("spring.h2.console.enabled", Boolean.class)).isFalse();
        }

        @Test
        void datasource_connectsWithDefaultCredentials() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                assertThat(connection.isValid(1)).isTrue();
                assertThat(connection.getMetaData().getUserName()).isEqualToIgnoringCase("sa");
            }
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
            "SPRING_DATASOURCE_USERNAME=platform",
            "SPRING_DATASOURCE_PASSWORD=from-environment",
            "SPRING_H2_CONSOLE_ENABLED=true",
            "spring.datasource.url=jdbc:h2:mem:datasource-credentials-test"
    })
    class WhenCredentialsAreProvidedExternally {

        @Autowired
        private Environment environment;

        @Autowired
        private DataSource dataSource;

        @Test
        void datasourceCredentials_areResolvedFromEnvironment() {
            assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("platform");
            assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("from-environment");
        }

        @Test
        void h2Console_canBeEnabledFromEnvironment() {
            assertThat(environment.getProperty("spring.h2.console.enabled", Boolean.class)).isTrue();
        }

        @Test
        void datasource_connectsWithExternallyProvidedCredentials() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                assertThat(connection.isValid(1)).isTrue();
                assertThat(connection.getMetaData().getUserName()).isEqualToIgnoringCase("platform");
            }
        }
    }

    private Properties loadApplicationProperties() throws IOException {
        final Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(in);
        }
        return properties;
    }

    private static MockEnvironment environmentWithApplicationProperties(Map<String, Object> environmentVariables) throws IOException {
        final MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("environmentVariables", environmentVariables));
        environment.getPropertySources().addLast(new ResourcePropertySource("classpath:application.properties"));
        return environment;
    }
}
