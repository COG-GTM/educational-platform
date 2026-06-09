package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the resource files required for bootRun to start the
 * application on port 8080 are present and contain essential configuration.
 * Without these files, bootRun would fail during DataSource or Liquibase
 * auto-configuration even though the Spring Boot plugin is correctly applied.
 */
class ApplicationPropertiesBootRunDefaultsTest {

    private static Path projectRoot;

    @BeforeAll
    static void findProjectRoot() {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;
    }

    @Test
    void applicationProperties_shouldExist_inConfigurationModule() {
        Path props = projectRoot.resolve("configuration/src/main/resources/application.properties");
        assertThat(Files.exists(props))
                .as("application.properties must exist for bootRun to resolve DataSource and Liquibase config")
                .isTrue();
    }

    @Test
    void applicationProperties_shouldConfigureDataSourceUrl() throws IOException {
        String content = Files.readString(
                projectRoot.resolve("configuration/src/main/resources/application.properties"));
        assertThat(content)
                .as("spring.datasource.url must be configured for bootRun to initialize the DataSource")
                .containsPattern("spring\\.datasource\\.url\\s*=");
    }

    @Test
    void applicationProperties_shouldConfigureH2Driver() throws IOException {
        String content = Files.readString(
                projectRoot.resolve("configuration/src/main/resources/application.properties"));
        assertThat(content)
                .as("H2 driver class must be configured for bootRun with embedded database")
                .contains("org.h2.Driver");
    }

    @Test
    void applicationProperties_shouldConfigureLiquibaseChangelog() throws IOException {
        String content = Files.readString(
                projectRoot.resolve("configuration/src/main/resources/application.properties"));
        assertThat(content)
                .as("Liquibase changelog path must be configured for bootRun database migrations")
                .containsPattern("spring\\.liquibase\\.change-log\\s*=");
    }

    @Test
    void liquibaseChangelogFile_shouldExist() {
        Path changelog = projectRoot.resolve(
                "configuration/src/main/resources/db/changelog/db.changelog-master.yml");
        assertThat(Files.exists(changelog))
                .as("Liquibase changelog master file must exist for bootRun database migrations")
                .isTrue();
    }

    @Test
    void applicationProperties_shouldConfigureJpaPlatform() throws IOException {
        String content = Files.readString(
                projectRoot.resolve("configuration/src/main/resources/application.properties"));
        assertThat(content)
                .as("JPA database platform must be configured for Hibernate dialect resolution on bootRun")
                .containsPattern("spring\\.jpa\\.database-platform\\s*=");
    }

    @Test
    void applicationProperties_shouldNotOverrideServerPort() throws IOException {
        String content = Files.readString(
                projectRoot.resolve("configuration/src/main/resources/application.properties"));
        assertThat(content)
                .as("server.port should not be set in application.properties — "
                        + "default port 8080 is documented in README for bootRun")
                .doesNotContain("server.port");
    }

    @Test
    void applicationSecurityProperties_shouldBeOnClasspath() {
        assertThat(getClass().getClassLoader().getResource("application-security.properties"))
                .as("application-security.properties must be on the classpath — "
                        + "referenced by @PropertySource on the application entry point")
                .isNotNull();
    }
}
