package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates the test slice annotation availability on the test classpath
 * after adding spring-boot-starter-test to the configuration module.
 * In Spring Boot 4.x, the test-autoconfigure module was restructured:
 * only {@code @JsonTest} and core infrastructure remain in
 * {@code spring-boot-test-autoconfigure}. Annotations like {@code @WebMvcTest}
 * and {@code @DataJpaTest} were moved to their respective starter modules
 * (e.g., spring-boot-web-test-autoconfigure, spring-boot-data-jpa-test-autoconfigure).
 * <p>
 * These tests document which test slice annotations ARE available from
 * spring-boot-starter-test alone, and which require additional starters.
 * This prevents developers from assuming all test slices are available
 * just because starter-test is on the classpath.
 * <p>
 * Complements {@link StarterTestWebUtilitiesValidationTest} (MockMvc utilities)
 * and {@link StarterTestTransitiveDependencyPresenceTest} (core transitives).
 */
class TestSliceAnnotationAvailabilityTest {

    @Test
    void jsonTest_shouldBeAvailableFromStarterTest() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.boot.test.autoconfigure.json.JsonTest");
            assertThat(clazz.isAnnotation())
                    .as("@JsonTest must be a resolvable annotation — "
                            + "it remains in spring-boot-test-autoconfigure in 4.x")
                    .isTrue();
        }).doesNotThrowAnyException();
    }

    @Test
    void autoConfigureJsonTesters_shouldBeAvailableFromStarterTest() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters");
            assertThat(clazz.isAnnotation())
                    .as("@AutoConfigureJsonTesters must be a resolvable annotation for JSON tester support")
                    .isTrue();
        }).doesNotThrowAnyException();
    }

    @Test
    void overrideAutoConfiguration_shouldBeAvailableFromStarterTest() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration");
            assertThat(clazz.isAnnotation())
                    .as("@OverrideAutoConfiguration must be available for test slice support")
                    .isTrue();
        }).doesNotThrowAnyException();
    }

    @Test
    void webMvcTest_shouldNotBeAvailableFromStarterTestAlone() {
        assertThatThrownBy(() -> Class.forName(
                "org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest"))
                .as("@WebMvcTest is NOT in spring-boot-test-autoconfigure in Spring Boot 4.x — "
                        + "it requires spring-boot-web-test-autoconfigure. Its absence here "
                        + "documents the 4.x test infrastructure restructuring.")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void dataJpaTest_shouldNotBeAvailableFromStarterTestAlone() {
        assertThatThrownBy(() -> Class.forName(
                "org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest"))
                .as("@DataJpaTest is NOT in spring-boot-test-autoconfigure in Spring Boot 4.x — "
                        + "it requires spring-boot-data-jpa-test-autoconfigure. Its absence here "
                        + "documents the 4.x test infrastructure restructuring.")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void jdbcTest_shouldNotBeAvailableFromStarterTestAlone() {
        assertThatThrownBy(() -> Class.forName(
                "org.springframework.boot.test.autoconfigure.jdbc.JdbcTest"))
                .as("@JdbcTest is NOT in spring-boot-test-autoconfigure in Spring Boot 4.x — "
                        + "it requires a separate test starter. Its absence here "
                        + "documents the 4.x test infrastructure restructuring.")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void testSliceTestContextBootstrapper_shouldBeAvailable() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper"))
                .as("TestSliceTestContextBootstrapper must be available — "
                        + "it is the core bootstrapper for all test slice annotations")
                .doesNotThrowAnyException();
    }
}
