package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Spring test context caching mechanism works correctly
 * with the application configuration. Spring's TestContext framework caches
 * {@code ApplicationContext} instances between test methods (and across test
 * classes with identical configuration) to avoid expensive re-initialization.
 * <p>
 * With the Spring Boot plugin and starter-test, context caching is critical
 * for test suite performance — without caching, every {@code @SpringBootTest}
 * would start a new application context (including embedded server, datasource,
 * Liquibase migrations), multiplying test execution time.
 * <p>
 * This test verifies the caching contract within a single test class: the same
 * context instance must be shared across all test methods, and it must remain
 * active throughout.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SpringBootTestContextCachingContractTest {

    private static ConfigurableApplicationContext firstContext;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Test
    @Order(1)
    void firstMethod_shouldReceiveActiveContext() {
        assertThat(applicationContext)
                .as("First test method must receive a non-null ApplicationContext")
                .isNotNull();
        assertThat(applicationContext.isActive())
                .as("ApplicationContext must be active in the first test method")
                .isTrue();
        firstContext = applicationContext;
    }

    @Test
    @Order(2)
    void secondMethod_shouldReceiveSameContextInstance() {
        assertThat((ApplicationContext) applicationContext)
                .as("Second test method must receive the same context instance — "
                        + "Spring TestContext framework must cache the context between "
                        + "test methods in the same class")
                .isSameAs(firstContext);
    }

    @Test
    @Order(3)
    void thirdMethod_shouldStillHaveActiveContext() {
        assertThat(applicationContext.isActive())
                .as("Context must remain active across all test methods — "
                        + "premature context closure would break subsequent tests")
                .isTrue();
        assertThat((ApplicationContext) applicationContext)
                .as("Third method must still reference the same cached context")
                .isSameAs(firstContext);
    }

    @Test
    @Order(4)
    void context_shouldContainApplicationBean_acrossAllMethods() {
        assertThat(applicationContext.containsBean("educationalPlatformApplication"))
                .as("Cached context must still contain the application bean — "
                        + "verifies the context is not degraded between test methods")
                .isTrue();
    }
}
