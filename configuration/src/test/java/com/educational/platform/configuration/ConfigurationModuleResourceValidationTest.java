package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that critical resource files required by the Spring Boot plugin's
 * bootRun and bootJar tasks are present on the classpath. The configuration
 * module is the composition root — if its application.properties is missing,
 * bootRun starts with Spring Boot defaults (random port, no datasource URL,
 * no Liquibase changelog), causing silent misconfiguration. If the Liquibase
 * changelog is missing, the context fails to start with a FileNotFoundException.
 */
class ConfigurationModuleResourceValidationTest {

    @Test
    void applicationProperties_shouldBeOnClasspath() {
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("application.properties");
        assertThat(stream)
                .as("application.properties must be on the classpath — "
                        + "bootRun and bootJar rely on it for datasource URL, "
                        + "Liquibase changelog location, and H2 console config")
                .isNotNull();
    }

    @Test
    void liquibaseChangelog_shouldBeOnClasspath() {
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("db/changelog/db.changelog-master.yml");
        assertThat(stream)
                .as("Liquibase master changelog must be on the classpath — "
                        + "spring.liquibase.change-log in application.properties references it; "
                        + "a missing changelog causes a startup failure")
                .isNotNull();
    }

    @Test
    void applicationSecurityProperties_shouldBeOnClasspath() {
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("application-security.properties");
        assertThat(stream)
                .as("application-security.properties must be on the classpath — "
                        + "@PropertySource on the application class references it "
                        + "with ignoreResourceNotFound=false (default), so a missing "
                        + "file causes a startup failure")
                .isNotNull();
    }
}
