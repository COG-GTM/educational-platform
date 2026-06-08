package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that JUnit 5 {@code @Nested} test classes inherit the
 * {@code @SpringBootTest} application context from the enclosing class.
 * This is a common testing pattern for grouping related assertions under
 * a shared context configuration. The Spring Boot test infrastructure
 * (provided by spring-boot-starter-test) must support context inheritance
 * across nested test classes without requiring redundant annotations.
 * <p>
 * Complements {@link SpringBootTestContextCachingContractTest} (context
 * caching across methods) and {@link MockitoBeanIntegrationTest} (bean
 * override support). This test validates the structural nesting pattern
 * that neither of those covers.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class SpringBootTestNestedClassSupportTest {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Test
    void outerClass_shouldHaveApplicationContext() {
        assertThat(applicationContext)
                .as("Outer test class must receive an injected ApplicationContext")
                .isNotNull();
        assertThat(applicationContext.isActive())
                .as("ApplicationContext must be active in outer class")
                .isTrue();
    }

    @Nested
    class WhenContextIsInherited {

        @Autowired
        private ConfigurableApplicationContext nestedContext;

        @Autowired
        private Environment environment;

        @Test
        void nestedClass_shouldInheritApplicationContext() {
            assertThat(nestedContext)
                    .as("@Nested class must inherit the ApplicationContext from the enclosing "
                            + "@SpringBootTest class without requiring its own annotation")
                    .isNotNull();
            assertThat(nestedContext.isActive())
                    .as("Inherited ApplicationContext must be active")
                    .isTrue();
        }

        @Test
        void nestedClass_shouldShareSameContextInstance() {
            assertThat(nestedContext)
                    .as("@Nested class must share the same cached context instance as the outer class")
                    .isSameAs(applicationContext);
        }

        @Test
        void nestedClass_shouldHaveAccessToEnvironment() {
            assertThat(environment)
                    .as("@Nested class must have access to the Spring Environment via @Autowired")
                    .isNotNull();
            assertThat(environment.getProperty("spring.datasource.url"))
                    .as("Environment must resolve properties from application.properties")
                    .isNotNull();
        }
    }

    @Nested
    class WhenApplicationBeansAreAccessed {

        @Autowired
        private ConfigurableApplicationContext innerContext;

        @Test
        void nestedClass_shouldAccessApplicationBean() {
            assertThat(innerContext.containsBean("educationalPlatformApplication"))
                    .as("@Nested class must be able to access application beans from the inherited context")
                    .isTrue();
        }

        @Test
        void nestedClass_shouldAccessDataSource() {
            assertThat(innerContext.containsBean("dataSource"))
                    .as("@Nested class must be able to access infrastructure beans like DataSource")
                    .isTrue();
        }
    }
}
