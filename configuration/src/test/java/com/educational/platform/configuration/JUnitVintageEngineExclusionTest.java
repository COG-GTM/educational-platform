package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates that JUnit 4 and the JUnit Vintage Engine are NOT on the test
 * classpath. Spring Boot 4.x's spring-boot-starter-test no longer includes
 * junit-vintage-engine. The project is exclusively JUnit 5 (Jupiter); having
 * JUnit 4 on the classpath would:
 * <ul>
 *   <li>Allow accidental JUnit 4 test authoring (@Test from org.junit)</li>
 *   <li>Add unnecessary classpath weight and startup time</li>
 *   <li>Risk hamcrest version conflicts between JUnit 4 and JUnit 5</li>
 * </ul>
 * Complements {@link BuildGradleTestFrameworkConfigurationTest} which validates
 * the build DSL, and {@link SpringBootStarterTestCoreInfrastructureTest} which
 * validates required test infrastructure IS present.
 */
class JUnitVintageEngineExclusionTest {

    @Test
    void junitVintageEngine_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName("org.junit.vintage.engine.VintageTestEngine"))
                .as("JUnit Vintage Engine must NOT be on the test classpath — "
                        + "the project uses JUnit 5 exclusively; vintage support "
                        + "would allow accidental JUnit 4 test creation")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void junit4TestAnnotation_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName("org.junit.Test"))
                .as("JUnit 4 @Test annotation must NOT be on the classpath — "
                        + "all tests must use org.junit.jupiter.api.Test")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void junit4Runner_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName("org.junit.runner.RunWith"))
                .as("JUnit 4 @RunWith must NOT be on the classpath — "
                        + "test execution uses JUnit Platform (Jupiter) exclusively")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void junit4Assert_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName("org.junit.Assert"))
                .as("JUnit 4 Assert must NOT be on the classpath — "
                        + "assertions use AssertJ (from starter-test) or JUnit 5 Assertions")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void junit5JupiterApi_shouldBeOnClasspath() {
        assertThat(org.junit.jupiter.api.Test.class)
                .as("JUnit 5 Jupiter API must be present on the test classpath")
                .isNotNull();
    }
}
