package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that {@code @SpringBootTest} correctly handles both the
 * {@code properties} attribute and the {@code args} attribute simultaneously.
 * {@link SpringBootTestArgsAttributeTest} covers args alone;
 * {@link SpringBootTestPropertyOverrideTest} covers properties alone.
 * This test verifies they coexist without interfering, and that Spring's
 * property source precedence holds: test properties (from {@code properties})
 * take highest precedence as TestPropertySource, followed by program args
 * (from {@code args}), followed by application.properties defaults.
 * <p>
 * This scenario mirrors a bootRun invocation with both
 * {@code --spring-boot.run.arguments=...} and SPRING_APPLICATION_JSON set.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "coexistence.from.properties=properties-value",
                "coexistence.override.key=from-properties"
        },
        args = {
                "--coexistence.from.args=args-value",
                "--coexistence.override.key=from-args"
        }
)
class SpringBootTestPropertyAndArgsCoexistenceTest {

    @Autowired
    private Environment environment;

    @Test
    void propertiesAttribute_shouldBeResolvable() {
        assertThat(environment.getProperty("coexistence.from.properties"))
                .as("Properties set via @SpringBootTest(properties = {...}) must be resolvable")
                .isEqualTo("properties-value");
    }

    @Test
    void argsAttribute_shouldBeResolvable() {
        assertThat(environment.getProperty("coexistence.from.args"))
                .as("Args set via @SpringBootTest(args = {...}) must be resolvable")
                .isEqualTo("args-value");
    }

    @Test
    void testProperties_shouldTakePrecedenceOverArgs_forSameKey() {
        // @SpringBootTest(properties = {...}) are added as TestPropertySource
        // which has the highest precedence in Spring's property source hierarchy,
        // even higher than program arguments.
        assertThat(environment.getProperty("coexistence.override.key"))
                .as("Test properties (from @SpringBootTest(properties = {...})) must take "
                        + "precedence over args for the same key — TestPropertySource has "
                        + "highest priority in the Spring test property source hierarchy")
                .isEqualTo("from-properties");
    }

    @Test
    void existingPropertySource_shouldStillBeLoaded() {
        assertThat(environment.containsProperty("com.educational.platform.security.enabled"))
                .as("@PropertySource(application-security.properties) must still load "
                        + "even when both properties and args are specified")
                .isTrue();
    }
}
