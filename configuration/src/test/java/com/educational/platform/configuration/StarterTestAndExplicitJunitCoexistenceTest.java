package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.Extension;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.launcher.Launcher;

import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the explicit JUnit dependencies declared in
 * configuration/build.gradle.kts (junit-jupiter-api, junit-platform-engine,
 * junit-platform-launcher) coexist correctly with the transitive JUnit
 * dependencies brought in by spring-boot-starter-test. When both explicit
 * and transitive JUnit versions are on the classpath, version conflicts can
 * cause NoSuchMethodError, ClassNotFoundException, or silent test skipping.
 * <p>
 * The Spring Boot BOM manages JUnit versions; by declaring explicit JUnit deps
 * WITHOUT a version (relying on BOM management), both paths should resolve
 * the same version. This test validates that assumption at runtime.
 * <p>
 * Complements {@link StarterTestJUnit5ExtensionModelTest} (extension point
 * contracts), {@link JUnitVintageEngineExclusionTest} (no vintage engine),
 * and {@link SpringBootTestJUnitPlatformCompatibilityTest} (platform compatibility).
 */
class StarterTestAndExplicitJunitCoexistenceTest {

    @Test
    void junitJupiterApi_shouldBeLoadableWithoutConflicts() {
        assertThatCode(() -> {
            Class<?> testAnnotation = Class.forName("org.junit.jupiter.api.Test");
            assertThat(testAnnotation.isAnnotation()).isTrue();
        })
                .as("org.junit.jupiter.api.Test must be loadable without version conflicts — "
                        + "conflicts between explicit and transitive JUnit deps would cause "
                        + "ClassNotFoundException or NoSuchMethodError")
                .doesNotThrowAnyException();
    }

    @Test
    void junitPlatformEngine_shouldBeLoadableWithoutConflicts() {
        assertThatCode(() -> {
            Class<?> engineClass = Class.forName("org.junit.platform.engine.TestEngine");
            assertThat(engineClass.isInterface()).isTrue();
        })
                .as("org.junit.platform.engine.TestEngine must be loadable — "
                        + "the explicit junit-platform-engine dependency must not conflict "
                        + "with the version brought by spring-boot-starter-test")
                .doesNotThrowAnyException();
    }

    @Test
    void junitPlatformLauncher_shouldBeLoadableWithoutConflicts() {
        assertThatCode(() -> {
            Class<?> launcherClass = Class.forName("org.junit.platform.launcher.Launcher");
            assertThat(launcherClass.isInterface()).isTrue();
        })
                .as("org.junit.platform.launcher.Launcher must be loadable — "
                        + "the explicit junit-platform-launcher dependency must not conflict "
                        + "with the version brought by spring-boot-starter-test")
                .doesNotThrowAnyException();
    }

    @Test
    void junitJupiterEngine_shouldBeDiscoverableViaServiceLoader() {
        ServiceLoader<TestEngine> engines = ServiceLoader.load(TestEngine.class);
        Set<String> engineIds = StreamSupport.stream(engines.spliterator(), false)
                .map(TestEngine::getId)
                .collect(Collectors.toSet());
        assertThat(engineIds)
                .as("JUnit Jupiter engine must be discoverable via ServiceLoader — "
                        + "if version conflicts exist, the engine may fail to load")
                .contains("junit-jupiter");
    }

    @Test
    void junitVintageEngine_shouldNotBePresent() {
        ServiceLoader<TestEngine> engines = ServiceLoader.load(TestEngine.class);
        Set<String> engineIds = StreamSupport.stream(engines.spliterator(), false)
                .map(TestEngine::getId)
                .collect(Collectors.toSet());
        assertThat(engineIds)
                .as("JUnit Vintage engine must NOT be on the classpath — "
                        + "spring-boot-starter-test excludes it by default in Spring Boot 3.x+, "
                        + "and the project uses JUnit 5 exclusively")
                .doesNotContain("junit-vintage");
    }

    @Test
    void extensionInterface_shouldBeFromSameJunitPlatformVersion() {
        Package extensionPkg = Extension.class.getPackage();
        Package testEnginePkg = TestEngine.class.getPackage();
        Package launcherPkg = Launcher.class.getPackage();

        // All JUnit Platform components should have consistent versions
        // (may be null in dev classpath but should be consistent if present)
        if (extensionPkg.getImplementationVersion() != null
                && testEnginePkg.getImplementationVersion() != null) {
            assertThat(extensionPkg.getImplementationVersion())
                    .as("JUnit extension and platform-engine must be from the same version — "
                            + "version mismatch indicates conflicting explicit/transitive deps")
                    .isEqualTo(testEnginePkg.getImplementationVersion());
        }
    }

    @Test
    void mockitoJunitJupiter_shouldWorkWithBothDependencyPaths() {
        assertThatCode(() -> {
            Class<?> mockitoExtension = Class.forName(
                    "org.mockito.junit.jupiter.MockitoExtension");
            assertThat(Extension.class.isAssignableFrom(mockitoExtension))
                    .as("MockitoExtension must implement JUnit 5 Extension interface — "
                            + "confirms mockito-junit-jupiter is compatible with the JUnit version")
                    .isTrue();
        }).doesNotThrowAnyException();
    }
}
