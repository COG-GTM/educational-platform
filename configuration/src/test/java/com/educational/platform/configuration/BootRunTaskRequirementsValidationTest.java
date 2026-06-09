package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Holistic validation of all prerequisites the Spring Boot Gradle plugin
 * requires for bootRun to succeed. Individual tests cover these aspects
 * separately; this test class validates them as a cohesive set to guard
 * against partial regressions where one requirement passes but the
 * combination fails (e.g., plugin applied but main class not detectable).
 */
class BootRunTaskRequirementsValidationTest {

    private static Path projectRoot;
    private static String configBuildContent;
    private static String tomlContent;

    @BeforeAll
    static void loadBuildFiles() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;
        configBuildContent = Files.readString(projectRoot.resolve("configuration/build.gradle.kts"));
        tomlContent = Files.readString(projectRoot.resolve("gradle/libs.versions.toml"));
    }

    @Test
    void requirement1_pluginDeclaredInVersionCatalog() {
        assertThat(tomlContent)
                .as("Requirement 1: springboot plugin must be declared in libs.versions.toml")
                .containsPattern("springboot.*org\\.springframework\\.boot");
    }

    @Test
    void requirement2_pluginAppliedInConfigurationModule() {
        assertThat(configBuildContent)
                .as("Requirement 2: springboot plugin must be applied in configuration/build.gradle.kts")
                .containsPattern("alias\\s*\\(\\s*libs\\.plugins\\.springboot\\s*\\)");
    }

    @Test
    void requirement3_mainClassAnnotatedWithSpringBootApplication() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(SpringBootApplication.class))
                .as("Requirement 3: application class must have @SpringBootApplication for auto-detection")
                .isTrue();
    }

    @Test
    void requirement4_mainMethodHasCorrectSignature() throws NoSuchMethodException {
        Method main = EducationalPlatformApplication.class.getDeclaredMethod("main", String[].class);
        assertThat(Modifier.isPublic(main.getModifiers()) && Modifier.isStatic(main.getModifiers()))
                .as("Requirement 4: main() must be public static for JVM entry point")
                .isTrue();
        assertThat(main.getReturnType())
                .as("Requirement 4: main() must return void")
                .isEqualTo(Void.TYPE);
    }

    @Test
    void requirement5_noMainClassOverride() {
        assertThat(configBuildContent)
                .as("Requirement 5: mainClass must not be overridden — plugin auto-detects via @SpringBootApplication")
                .doesNotContainPattern("mainClass\\s*[.=]");
    }

    @Test
    void requirement6_bootRunNotDisabled() {
        assertThat(configBuildContent)
                .as("Requirement 6: bootRun task must not be disabled")
                .doesNotContainPattern("bootRun.*enabled\\s*=\\s*false");
    }

    @Test
    void requirement7_pluginNotAppliedWithApplyFalse() {
        assertThat(configBuildContent)
                .as("Requirement 7: plugin must not use 'apply false' which prevents task registration")
                .doesNotContainPattern("apply\\s*=?\\s*false");
    }

    @Test
    void requirement8_starterWebDependencyPresent() {
        assertThat(configBuildContent)
                .as("Requirement 8: spring-boot-starter-web must be present for embedded server")
                .contains("spring-boot-starter-web");
    }

    @Test
    void requirement9_applicationClassInBasePackage() {
        assertThat(EducationalPlatformApplication.class.getPackageName())
                .as("Requirement 9: application class must be in base package for component scanning")
                .isEqualTo("com.educational.platform");
    }

    @Test
    void requirement10_pluginBlockBeforeDependencies() {
        int pluginsIdx = configBuildContent.indexOf("plugins");
        int depsIdx = configBuildContent.indexOf("dependencies");
        assertThat(pluginsIdx)
                .as("Requirement 10: plugins block must precede dependencies (Gradle DSL requirement)")
                .isLessThan(depsIdx);
    }
}
