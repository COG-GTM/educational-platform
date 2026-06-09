package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the source set structure of the configuration module.
 * As the composition root, the configuration module must contain only the
 * Spring Boot application entry point and essential resources (application
 * properties, Liquibase changelog). Business logic belongs in domain modules.
 * If additional Java classes are added here, it violates the modular monolith
 * architecture and makes it harder to extract modules into microservices.
 */
class ConfigurationModuleSourceSetStructureTest {

    private static Path projectRoot;
    private static Path configModule;

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
        configModule = projectRoot.resolve("configuration");
    }

    @Test
    void mainJavaSource_shouldContainExactlyOneJavaFile() throws IOException {
        Path mainJava = configModule.resolve("src/main/java");
        assertThat(Files.exists(mainJava))
                .as("configuration/src/main/java must exist")
                .isTrue();
        try (Stream<Path> javaFiles = Files.walk(mainJava)
                .filter(p -> p.toString().endsWith(".java"))) {
            List<Path> files = javaFiles.toList();
            assertThat(files)
                    .as("Configuration module must contain exactly one Java source file "
                            + "(the application entry point) — business logic belongs in domain modules")
                    .hasSize(1);
            assertThat(files.get(0).getFileName().toString())
                    .as("The sole Java file must be EducationalPlatformApplication.java")
                    .isEqualTo("EducationalPlatformApplication.java");
        }
    }

    @Test
    void mainResources_shouldContainApplicationProperties() {
        Path appProps = configModule.resolve("src/main/resources/application.properties");
        assertThat(Files.exists(appProps))
                .as("application.properties must exist for Spring Boot auto-configuration "
                        + "(datasource, Liquibase, H2 console)")
                .isTrue();
    }

    @Test
    void mainResources_shouldContainLiquibaseChangelog() {
        Path changelog = configModule.resolve(
                "src/main/resources/db/changelog/db.changelog-master.yml");
        assertThat(Files.exists(changelog))
                .as("Liquibase master changelog must exist for database migration at bootRun startup")
                .isTrue();
    }

    @Test
    void mainSource_shouldNotContainControllerClasses() throws IOException {
        Path mainJava = configModule.resolve("src/main/java");
        try (Stream<Path> javaFiles = Files.walk(mainJava)
                .filter(p -> p.toString().endsWith(".java"))) {
            List<String> controllerFiles = javaFiles
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.contains("Controller"))
                    .toList();
            assertThat(controllerFiles)
                    .as("Configuration module must NOT contain controller classes — "
                            + "REST endpoints belong in domain module web layers")
                    .isEmpty();
        }
    }

    @Test
    void mainSource_shouldNotContainServiceClasses() throws IOException {
        Path mainJava = configModule.resolve("src/main/java");
        try (Stream<Path> javaFiles = Files.walk(mainJava)
                .filter(p -> p.toString().endsWith(".java"))) {
            List<String> serviceFiles = javaFiles
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.contains("Service"))
                    .toList();
            assertThat(serviceFiles)
                    .as("Configuration module must NOT contain service classes — "
                            + "business logic belongs in domain module application layers")
                    .isEmpty();
        }
    }

    @Test
    void mainSource_shouldNotContainRepositoryClasses() throws IOException {
        Path mainJava = configModule.resolve("src/main/java");
        try (Stream<Path> javaFiles = Files.walk(mainJava)
                .filter(p -> p.toString().endsWith(".java"))) {
            List<String> repositoryFiles = javaFiles
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.contains("Repository"))
                    .toList();
            assertThat(repositoryFiles)
                    .as("Configuration module must NOT contain repository classes — "
                            + "data access belongs in domain module application layers")
                    .isEmpty();
        }
    }

    @Test
    void mainSource_shouldNotContainConfigurationClasses() throws IOException {
        Path mainJava = configModule.resolve("src/main/java");
        try (Stream<Path> javaFiles = Files.walk(mainJava)
                .filter(p -> p.toString().endsWith(".java"))) {
            List<String> configFiles = javaFiles
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith("Config.java") || name.endsWith("Configuration.java"))
                    .filter(name -> !name.equals("EducationalPlatformApplication.java"))
                    .toList();
            assertThat(configFiles)
                    .as("Configuration module must NOT contain @Configuration classes — "
                            + "module-specific configuration belongs in the respective module")
                    .isEmpty();
        }
    }
}
