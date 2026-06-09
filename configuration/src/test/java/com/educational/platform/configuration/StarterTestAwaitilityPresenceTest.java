package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that Awaitility — a testing utility for asynchronous operations —
 * is available on the test classpath via spring-boot-starter-test. The
 * application uses {@code @EnableAsync} for inter-module event publishing;
 * Awaitility provides the {@code await().atMost(...).until(...)} DSL that is
 * essential for testing asynchronous event handlers without brittle
 * {@code Thread.sleep()} calls.
 * <p>
 * Complements {@link StarterTestTransitiveDependencyPresenceTest} (which
 * validates AssertJ, Mockito, Hamcrest, JSONassert) and
 * {@link TestInfrastructureConfigurationTest} (which validates JsonPath and
 * MockMvc). Awaitility was not covered by either of those tests despite being
 * a key transitive of spring-boot-starter-test since Spring Boot 2.x.
 */
class StarterTestAwaitilityPresenceTest {

    @Test
    void awaitilityClass_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName("org.awaitility.Awaitility"))
                .as("Awaitility must be on the test classpath — "
                        + "spring-boot-starter-test includes awaitility as a transitive "
                        + "dependency; the project uses @EnableAsync, making async test "
                        + "utilities essential for testing event handlers")
                .doesNotThrowAnyException();
    }

    @Test
    void awaitilityConditionFactory_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName("org.awaitility.core.ConditionFactory"))
                .as("Awaitility ConditionFactory must be loadable — "
                        + "it is the core type returned by Awaitility.await()")
                .doesNotThrowAnyException();
    }

    @Test
    void awaitilityPollInterval_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName("org.awaitility.pollinterval.PollInterval"))
                .as("Awaitility PollInterval must be loadable for configuring poll intervals")
                .doesNotThrowAnyException();
    }

    @Test
    void awaitilityClass_shouldExposeStaticAwaitMethod() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName("org.awaitility.Awaitility");
            assertThat(clazz.getMethod("await"))
                    .as("Awaitility.await() static method must be available")
                    .isNotNull();
        }).doesNotThrowAnyException();
    }
}
