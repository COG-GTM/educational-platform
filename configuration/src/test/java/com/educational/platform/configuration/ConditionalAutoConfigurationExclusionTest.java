package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the ApplicationContextRunner from starter-test supports
 * selective configuration inclusion and exclusion. The Spring Boot plugin
 * enables auto-configuration; these tests verify that the test tooling
 * allows fine-grained control over which configurations are active,
 * which is critical for focused slice testing and troubleshooting.
 */
class ConditionalAutoConfigurationExclusionTest {

    @Test
    void contextRunner_withConfiguration_shouldRegisterBean() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestDataConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("testDataValue");
                    assertThat(context.getBean("testDataValue", String.class))
                            .isEqualTo("present");
                });
    }

    @Test
    void contextRunner_withoutConfiguration_shouldNotHaveBean() {
        new ApplicationContextRunner()
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("testDataValue");
                });
    }

    @Test
    void contextRunner_shouldSupportMultipleConfigurations() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestDataConfig.class, AnotherTestConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("testDataValue");
                    assertThat(context).hasBean("anotherValue");
                });
    }

    @Test
    void contextRunner_shouldSupportSelectiveConfigurationInclusion() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestDataConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("testDataValue");
                    assertThat(context).doesNotHaveBean("anotherValue");
                });
    }

    @Test
    void contextRunner_shouldSupportPropertyBasedConditions() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.main.lazy-initialization=true")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void contextRunner_shouldSupportMultiplePropertyValues() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "app.key1=value1",
                        "app.key2=value2"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getEnvironment().getProperty("app.key1"))
                            .isEqualTo("value1");
                    assertThat(context.getEnvironment().getProperty("app.key2"))
                            .isEqualTo("value2");
                });
    }

    @Configuration
    static class TestDataConfig {
        @Bean
        String testDataValue() {
            return "present";
        }
    }

    @Configuration
    static class AnotherTestConfig {
        @Bean
        String anotherValue() {
            return "another";
        }
    }
}
