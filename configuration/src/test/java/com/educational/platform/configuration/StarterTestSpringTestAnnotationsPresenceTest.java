package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that core spring-test annotations are available on the test
 * classpath via the spring-boot-starter-test dependency. Existing tests
 * cover Spring Boot-specific annotations ({@code @SpringBootTest},
 * {@code @MockitoBean}, {@code @DynamicPropertySource}); this test covers
 * the foundational spring-test framework annotations that are also
 * transitively provided by starter-test and commonly used alongside
 * {@code @SpringBootTest} for test isolation and data management.
 * <p>
 * If these annotations are missing, developers would get confusing
 * compilation errors when writing standard Spring integration tests
 * with database setup/teardown.
 */
class StarterTestSpringTestAnnotationsPresenceTest {

    @Test
    void dirtiesContext_shouldBeAvailable() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.test.annotation.DirtiesContext");
            assertThat(clazz.isAnnotation())
                    .as("@DirtiesContext must be an annotation type")
                    .isTrue();
        })
                .as("@DirtiesContext must be on test classpath via starter-test — "
                        + "it is used to reset the ApplicationContext between tests "
                        + "that modify shared state")
                .doesNotThrowAnyException();
    }

    @Test
    void commit_shouldBeAvailable() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.test.annotation.Commit");
            assertThat(clazz.isAnnotation())
                    .as("@Commit must be an annotation type")
                    .isTrue();
        })
                .as("@Commit must be on test classpath via starter-test — "
                        + "it commits database transactions after test methods instead of rolling back")
                .doesNotThrowAnyException();
    }

    @Test
    void rollback_shouldBeAvailable() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.test.annotation.Rollback");
            assertThat(clazz.isAnnotation())
                    .as("@Rollback must be an annotation type")
                    .isTrue();
        })
                .as("@Rollback must be on test classpath via starter-test — "
                        + "it controls transaction rollback behavior in integration tests")
                .doesNotThrowAnyException();
    }

    @Test
    void sql_shouldBeAvailable() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.test.context.jdbc.Sql");
            assertThat(clazz.isAnnotation())
                    .as("@Sql must be an annotation type")
                    .isTrue();
        })
                .as("@Sql must be on test classpath via starter-test — "
                        + "it executes SQL scripts for test data setup/teardown "
                        + "in database integration tests")
                .doesNotThrowAnyException();
    }

    @Test
    void testPropertySource_shouldBeAvailable() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.test.context.TestPropertySource");
            assertThat(clazz.isAnnotation())
                    .as("@TestPropertySource must be an annotation type")
                    .isTrue();
        })
                .as("@TestPropertySource must be on test classpath via starter-test — "
                        + "it overrides application properties in tests for isolation")
                .doesNotThrowAnyException();
    }

    @Test
    void activeProfiles_shouldBeAvailable() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.test.context.ActiveProfiles");
            assertThat(clazz.isAnnotation())
                    .as("@ActiveProfiles must be an annotation type")
                    .isTrue();
        })
                .as("@ActiveProfiles must be on test classpath via starter-test — "
                        + "it activates Spring profiles declaratively in test classes")
                .doesNotThrowAnyException();
    }

    @Test
    void contextConfiguration_shouldBeAvailable() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.test.context.ContextConfiguration");
            assertThat(clazz.isAnnotation())
                    .as("@ContextConfiguration must be an annotation type")
                    .isTrue();
        })
                .as("@ContextConfiguration must be on test classpath via starter-test — "
                        + "it is the foundational annotation for loading Spring contexts in tests")
                .doesNotThrowAnyException();
    }
}
