package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that key web testing utilities provided by spring-boot-starter-test
 * and its transitive dependencies are available on the test classpath.
 * The PR added the starter-test dependency; these tests guard against
 * regressions where the dependency might be removed or its scope changed,
 * breaking web-layer and integration testing capabilities.
 * Complements SpringBootStarterTestValidationTest which covers core test infra.
 */
class StarterTestWebUtilitiesValidationTest {

    @Test
    void mockMvc_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.test.web.servlet.MockMvc"))
                .as("MockMvc must be available for web-layer unit testing without a full server")
                .doesNotThrowAnyException();
    }

    @Test
    void mockMvcRequestBuilders_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.test.web.servlet.request.MockMvcRequestBuilders"))
                .as("MockMvcRequestBuilders must be available for constructing mock HTTP requests")
                .doesNotThrowAnyException();
    }

    @Test
    void mockMvcResultMatchers_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.test.web.servlet.result.MockMvcResultMatchers"))
                .as("MockMvcResultMatchers must be available for asserting HTTP response content")
                .doesNotThrowAnyException();
    }

    @Test
    void webApplicationContextRunner_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.test.context.runner.WebApplicationContextRunner"))
                .as("WebApplicationContextRunner must be available for web context testing")
                .doesNotThrowAnyException();
    }

    @Test
    void testPropertyValues_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.test.util.TestPropertyValues"))
                .as("TestPropertyValues must be available for programmatic property injection")
                .doesNotThrowAnyException();
    }

    @Test
    void filteredClassLoader_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.test.context.FilteredClassLoader"))
                .as("FilteredClassLoader must be available for auto-config conditional testing")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootMockServletContext_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.test.mock.web.SpringBootMockServletContext"))
                .as("SpringBootMockServletContext must be available for web testing support")
                .doesNotThrowAnyException();
    }

    @Test
    void jsonTest_shouldBeAnAnnotation() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.boot.test.autoconfigure.json.JsonTest");
            assertThat(clazz.isAnnotation())
                    .as("@JsonTest must be a resolvable annotation for JSON slice testing")
                    .isTrue();
        }).doesNotThrowAnyException();
    }
}
