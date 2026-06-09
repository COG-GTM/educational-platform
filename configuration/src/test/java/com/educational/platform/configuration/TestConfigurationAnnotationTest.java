package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that @TestConfiguration — a key annotation from spring-boot-test
 * for providing test-specific bean definitions — is available and functional.
 * The PR added spring-boot-starter-test which provides this annotation;
 * without it, tests requiring custom bean overrides or additional beans
 * for specific scenarios would have no Spring Boot–aware mechanism to do so.
 * <p>
 * Complements {@link MockitoBeanIntegrationTest} (bean replacement via mock)
 * and {@link TestSliceAnnotationFunctionalTest} (test infrastructure utilities).
 */
class TestConfigurationAnnotationTest {

    @TestConfiguration
    static class AdditionalTestBeans {
        @Bean
        String testMarkerBean() {
            return "test-marker-value";
        }
    }

    @Test
    void testConfigurationAnnotation_shouldBeAvailableOnClasspath() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.boot.test.context.TestConfiguration");
            assertThat(clazz.isAnnotation())
                    .as("@TestConfiguration must be a resolvable annotation")
                    .isTrue();
        }).doesNotThrowAnyException();
    }

    @Test
    void testConfiguration_shouldNotBePickedUpByComponentScan() {
        TestConfiguration tc = AdditionalTestBeans.class.getAnnotation(TestConfiguration.class);
        assertThat(tc)
                .as("@TestConfiguration must be present on the inner class")
                .isNotNull();
    }

    @Test
    void testConfiguration_shouldProvideBeansViaContextRunner() {
        new ApplicationContextRunner()
                .withUserConfiguration(AdditionalTestBeans.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("testMarkerBean", String.class))
                            .as("@TestConfiguration inner class must register its beans in the test context")
                            .isEqualTo("test-marker-value");
                });
    }

    @TestConfiguration
    static class MultiBeanConfig {
        @Bean
        String beanA() { return "A"; }
        @Bean
        Integer beanB() { return 42; }
    }

    @Test
    void testConfiguration_shouldSupportMultipleBeanDefinitions() {
        new ApplicationContextRunner()
                .withUserConfiguration(MultiBeanConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("beanA", String.class)).isEqualTo("A");
                    assertThat(context.getBean("beanB", Integer.class)).isEqualTo(42);
                });
    }
}
