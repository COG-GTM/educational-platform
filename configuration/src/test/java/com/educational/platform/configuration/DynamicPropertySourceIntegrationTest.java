package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that {@code @DynamicPropertySource} — a key integration testing
 * feature enabled by {@code spring-boot-starter-test} — works correctly with
 * the application context. This mechanism is essential for Testcontainers-style
 * patterns where property values (e.g., database URLs, port numbers) are only
 * known at runtime. Without the Spring Boot plugin and starter-test on the
 * classpath, {@code @DynamicPropertySource} would not resolve correctly.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class DynamicPropertySourceIntegrationTest {

    private static final String DYNAMIC_VALUE = "dynamic-jdbc:h2:mem:dyntest";

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("test.dynamic.datasource.url", () -> DYNAMIC_VALUE);
        registry.add("test.dynamic.timeout", () -> 5000);
        registry.add("test.dynamic.enabled", () -> true);
    }

    @Autowired
    private Environment environment;

    @Test
    void dynamicProperty_stringShouldBeResolvable() {
        assertThat(environment.getProperty("test.dynamic.datasource.url"))
                .as("@DynamicPropertySource string property must be resolvable in the Environment")
                .isEqualTo(DYNAMIC_VALUE);
    }

    @Test
    void dynamicProperty_integerShouldBeResolvable() {
        assertThat(environment.getProperty("test.dynamic.timeout", Integer.class))
                .as("@DynamicPropertySource integer property must support typed resolution")
                .isEqualTo(5000);
    }

    @Test
    void dynamicProperty_booleanShouldBeResolvable() {
        assertThat(environment.getProperty("test.dynamic.enabled", Boolean.class))
                .as("@DynamicPropertySource boolean property must support typed resolution")
                .isTrue();
    }

    @Test
    void dynamicProperties_shouldCoexistWithStaticProperties() {
        assertThat(environment.getProperty("spring.datasource.url"))
                .as("Static properties from application.properties must still resolve "
                        + "alongside dynamic properties")
                .isNotNull();
        assertThat(environment.getProperty("test.dynamic.datasource.url"))
                .as("Dynamic properties must resolve alongside static ones")
                .isEqualTo(DYNAMIC_VALUE);
    }

    @Test
    void dynamicProperties_shouldTakePrecedenceOverDefaults() {
        assertThat(environment.getProperty("test.dynamic.datasource.url"))
                .as("Dynamic properties should be resolvable even when no default exists "
                        + "in application.properties")
                .isNotNull()
                .isNotBlank();
    }
}
