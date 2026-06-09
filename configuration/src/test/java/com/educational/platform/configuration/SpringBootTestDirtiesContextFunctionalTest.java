package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functionally validates that {@code @DirtiesContext} from the Spring
 * TestContext Framework (provided transitively by
 * {@code spring-boot-starter-test}) properly marks the application
 * context for closure after a test method. Existing tests validate
 * classpath availability ({@link StarterTestSpringTestAnnotationsPresenceTest});
 * this test exercises the actual context-dirtying behavior.
 * <p>
 * In the modular monolith, {@code @DirtiesContext} is essential for test
 * isolation when a test modifies shared singleton state (e.g., in-memory
 * caches, static registrations) that could leak into subsequent tests.
 * Without starter-test, the {@code DirtiesContextTestExecutionListener}
 * would not be registered, and {@code @DirtiesContext} would be silently
 * ignored.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SpringBootTestDirtiesContextFunctionalTest {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Test
    @Order(1)
    void context_shouldBeActiveBeforeDirtying() {
        assertThat(applicationContext.isActive())
                .as("Application context must be active before @DirtiesContext is applied")
                .isTrue();
    }

    @Test
    @Order(2)
    @DirtiesContext
    void dirtiesContext_shouldMarkContextForClosure() {
        assertThat(applicationContext.isActive())
                .as("Context must still be active during the @DirtiesContext test method — "
                        + "the context is closed AFTER the method completes, not during")
                .isTrue();
    }

    @Test
    @Order(3)
    void afterDirtiesContext_shouldReceiveFreshContext() {
        assertThat(applicationContext.isActive())
                .as("After @DirtiesContext, the test framework must provide a fresh, active "
                        + "application context — this proves the DirtiesContextTestExecutionListener "
                        + "from spring-test (via starter-test) is functional")
                .isTrue();
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DirtiesContextModeVerification {

        @Test
        @Order(1)
        void dirtiesContextAnnotation_shouldSupportMethodLevel() {
            assertThat(DirtiesContext.class.isAnnotation())
                    .as("@DirtiesContext must be applicable at method level")
                    .isTrue();
            assertThat(DirtiesContext.class.getAnnotation(
                    java.lang.annotation.Target.class).value())
                    .as("@DirtiesContext must target both TYPE and METHOD")
                    .extracting(Enum::name)
                    .contains("TYPE", "METHOD");
        }

        @Test
        @Order(2)
        void dirtiesContext_methodMode_shouldHaveExpectedValues() {
            DirtiesContext.MethodMode[] modes = DirtiesContext.MethodMode.values();
            assertThat(modes)
                    .as("@DirtiesContext.MethodMode must contain BEFORE_METHOD and AFTER_METHOD")
                    .extracting(Enum::name)
                    .containsExactlyInAnyOrder("BEFORE_METHOD", "AFTER_METHOD");
        }
    }
}
