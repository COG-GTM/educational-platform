package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functionally validates the {@link TestPropertyValues} utility class from
 * {@code spring-boot-test}. Existing tests
 * ({@link TestSliceAnnotationFunctionalTest}, {@link StarterTestWebUtilitiesValidationTest})
 * verify classpath presence; this test exercises actual property injection
 * into a {@link ConfigurableEnvironment} and into an
 * {@link ApplicationContextRunner}, proving the test utility from
 * {@code spring-boot-starter-test} is fully functional — not just loadable.
 * <p>
 * {@code TestPropertyValues} is used in auto-configuration tests to inject
 * properties without loading the full Spring context. If the utility's
 * internal property-source registration is broken, auto-configuration tests
 * in downstream modules would silently use wrong defaults.
 */
class StarterTestPropertyValuesIntegrationTest {

    @Test
    void applyTo_shouldInjectPropertyIntoEnvironment() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        TestPropertyValues.of("test.app.name=educational-platform")
                .applyTo(environment);

        assertThat(environment.getProperty("test.app.name"))
                .as("TestPropertyValues.applyTo(Environment) must inject the property — "
                        + "this proves the utility can modify Spring's property resolution")
                .isEqualTo("educational-platform");
    }

    @Test
    void applyTo_shouldSupportMultipleProperties() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        TestPropertyValues.of(
                "db.host=localhost",
                "db.port=5432",
                "db.name=testdb"
        ).applyTo(environment);

        assertThat(environment.getProperty("db.host")).isEqualTo("localhost");
        assertThat(environment.getProperty("db.port")).isEqualTo("5432");
        assertThat(environment.getProperty("db.name")).isEqualTo("testdb");
    }

    @Test
    void applyTo_shouldOverrideExistingProperties() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        TestPropertyValues.of("test.key=original").applyTo(environment);
        TestPropertyValues.of("test.key=overridden").applyTo(environment);

        assertThat(environment.getProperty("test.key"))
                .as("Applying TestPropertyValues twice must override the first value — "
                        + "this is important for test isolation where different tests "
                        + "need different property values")
                .isEqualTo("overridden");
    }

    @Test
    void applyTo_withApplicationContextRunner_shouldMakePropertiesAvailable() {
        new ApplicationContextRunner()
                .withPropertyValues("custom.property=runner-value")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getEnvironment().getProperty("custom.property"))
                            .as("Properties injected via ApplicationContextRunner must be resolvable")
                            .isEqualTo("runner-value");
                });
    }

    @Test
    void applyTo_shouldSupportKeyValuePairFormat() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        TestPropertyValues.of("spring.profiles.active=test")
                .applyTo(environment);

        assertThat(environment.getProperty("spring.profiles.active"))
                .as("TestPropertyValues must support key=value pair format")
                .isEqualTo("test");
    }
}
