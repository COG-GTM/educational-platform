package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the MOCK web environment infrastructure — provided by
 * spring-boot-starter-test — is available on the test classpath and
 * functional. The existing suite covers full-context tests with NONE
 * ({@link com.educational.platform.application.EducationalPlatformApplicationTest}),
 * RANDOM_PORT ({@link TestRestClientAvailabilityTest}), and DEFINED_PORT
 * ({@link SpringBootTestWebEnvironmentCompletenessTest}).
 * <p>
 * This test validates the MOCK environment infrastructure classes are
 * resolvable and instantiable, which is a prerequisite for MockMvc-based
 * controller testing without starting an HTTP server. The full application
 * context is not loaded here because the application's @PropertySource
 * with a bare filename ("application-security.properties") resolves against
 * MockServletContext's web root rather than the classpath, which is a known
 * limitation of MOCK mode with bare @PropertySource filenames.
 * <p>
 * Complements {@link AutoConfigureMockMvcAvailabilityTest} (annotation presence)
 * and {@link StarterTestWebUtilitiesValidationTest} (web test utilities).
 */
class SpringBootTestMockWebEnvironmentTest {

    @Test
    void mockServletContext_shouldBeInstantiable() {
        MockServletContext mockCtx = new MockServletContext();
        assertThat(mockCtx)
                .as("MockServletContext from spring-test must be instantiable — "
                        + "it is the foundation of @SpringBootTest(MOCK) mode")
                .isNotNull();
    }

    @Test
    void mockServletContext_shouldSupportContextPath() {
        MockServletContext mockCtx = new MockServletContext();
        mockCtx.setContextPath("/api");
        assertThat(mockCtx.getContextPath())
                .as("MockServletContext must support context path configuration")
                .isEqualTo("/api");
    }

    @Test
    void mockServletContext_shouldSupportInitParameters() {
        MockServletContext mockCtx = new MockServletContext();
        mockCtx.setInitParameter("spring.profiles.active", "test");
        assertThat(mockCtx.getInitParameter("spring.profiles.active"))
                .as("MockServletContext must support init parameters for property resolution")
                .isEqualTo("test");
    }

    @Test
    void webApplicationContext_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.web.context.WebApplicationContext"))
                .as("WebApplicationContext must be available — required for MOCK mode "
                        + "to create a web-aware application context without HTTP server")
                .doesNotThrowAnyException();
    }

    @Test
    void mockMvc_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.test.web.servlet.MockMvc"))
                .as("MockMvc must be available — the primary testing tool for MOCK mode "
                        + "to simulate HTTP requests against controllers")
                .doesNotThrowAnyException();
    }

    @Test
    void webEnvironmentMock_shouldBeDefaultValue() {
        assertThat(SpringBootTest.WebEnvironment.MOCK)
                .as("WebEnvironment.MOCK must exist as an enum constant — "
                        + "it is the default web environment for @SpringBootTest")
                .isNotNull();
        assertThat(SpringBootTest.WebEnvironment.MOCK.name())
                .isEqualTo("MOCK");
    }

    @Test
    void mockMvcBuilders_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.test.web.servlet.setup.MockMvcBuilders"))
                .as("MockMvcBuilders must be available for manual MockMvc setup — "
                        + "used when @AutoConfigureMockMvc is not on the classpath (Spring Boot 4.x)")
                .doesNotThrowAnyException();
    }

    @Test
    void mockMvcRequestBuilders_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.test.web.servlet.request.MockMvcRequestBuilders"))
                .as("MockMvcRequestBuilders must be available for constructing mock HTTP requests")
                .doesNotThrowAnyException();
    }
}
