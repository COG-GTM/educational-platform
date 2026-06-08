package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the @PropertySource("application-security.properties") annotation
 * on the application entry point actually loads the security configuration into
 * the Spring Environment. Without the Spring Boot plugin, the context cannot
 * initialize and these properties would never be loaded.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class SecurityPropertyLoadingTest {

    @Autowired
    private Environment environment;

    @Test
    void securityEnabledProperty_shouldBeLoaded() {
        String value = environment.getProperty("com.educational.platform.security.enabled");
        assertThat(value)
                .as("@PropertySource must load application-security.properties with security.enabled")
                .isNotNull()
                .isEqualTo("true");
    }

    @Test
    void securityEnabledProperty_shouldResolveToBooleanTrue() {
        Boolean enabled = environment.getProperty("com.educational.platform.security.enabled", Boolean.class);
        assertThat(enabled)
                .as("Security enabled property must resolve to Boolean true")
                .isTrue();
    }

    @Test
    void environment_shouldContainApplicationProperties() {
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        assertThat(datasourceUrl)
                .as("application.properties should be loaded by Spring Boot auto-configuration")
                .isNotNull()
                .contains("h2");
    }

    @Test
    void environment_shouldContainLiquibaseConfiguration() {
        String changeLog = environment.getProperty("spring.liquibase.change-log");
        assertThat(changeLog)
                .as("Liquibase changelog property should be loaded from application.properties")
                .isNotNull()
                .contains("db.changelog-master");
    }
}
