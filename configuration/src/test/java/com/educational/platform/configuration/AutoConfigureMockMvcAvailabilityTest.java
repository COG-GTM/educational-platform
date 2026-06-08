package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates that MockMvc can be manually configured from the
 * WebApplicationContext when the Spring Boot plugin is applied.
 * In Spring Boot 4.x, @AutoConfigureMockMvc was moved to
 * spring-boot-web-test-autoconfigure (separate from starter-test);
 * this test validates the manual MockMvc setup path that remains
 * available via spring-test (transitive of starter-test).
 * <p>
 * Uses RANDOM_PORT to avoid the MOCK servlet context issue where
 * @PropertySource resource resolution fails against src/main/webapp.
 * <p>
 * Complements {@link StarterTestWebUtilitiesValidationTest} (classpath checks)
 * and {@link TestSliceAnnotationAvailabilityTest} (4.x annotation availability).
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class AutoConfigureMockMvcAvailabilityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void webApplicationContext_shouldBeAvailableForMockMvcSetup() {
        assertThat(webApplicationContext)
                .as("WebApplicationContext must be injected — "
                        + "it is the foundation for building MockMvc instances manually")
                .isNotNull();
    }

    @Test
    void mockMvc_shouldBeManuallyBuildableFromWebContext() {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        assertThat(mockMvc)
                .as("MockMvc must be constructable from WebApplicationContext — "
                        + "this is the manual setup path available via spring-test "
                        + "when @AutoConfigureMockMvc is not on the classpath")
                .isNotNull();
    }

    @Test
    void mockMvc_shouldBeUsableForRequests() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        var result = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/"));
        assertThat(result)
                .as("Manually-built MockMvc must be able to perform HTTP requests")
                .isNotNull();
    }

    @Test
    void autoConfigureMockMvc_shouldNotBeInStarterTestAlone() {
        assertThatThrownBy(() -> Class.forName(
                "org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc"))
                .as("@AutoConfigureMockMvc is NOT in spring-boot-test-autoconfigure in 4.x — "
                        + "it requires spring-boot-web-test-autoconfigure. Its absence here "
                        + "documents the 4.x restructuring and guides developers to use "
                        + "manual MockMvc setup or add the web test starter.")
                .isInstanceOf(ClassNotFoundException.class);
    }
}
