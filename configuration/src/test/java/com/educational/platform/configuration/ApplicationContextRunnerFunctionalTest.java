package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functional tests for the ApplicationContextRunner provided by
 * spring-boot-starter-test. Unlike SpringBootStarterTestValidationTest
 * (which only checks classpath availability), these tests prove the
 * test infrastructure actually works — runners create contexts,
 * register beans, and resolve properties.
 */
class ApplicationContextRunnerFunctionalTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void applicationContextRunner_shouldCreateContextWithoutFailure() {
        contextRunner.run(context ->
                assertThat(context).hasNotFailed()
        );
    }

    @Test
    void applicationContextRunner_shouldRegisterUserConfiguration() {
        contextRunner
                .withUserConfiguration(SampleTestConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SampleTestConfig.class);
                });
    }

    @Test
    void applicationContextRunner_shouldResolveBeansByType() {
        contextRunner
                .withUserConfiguration(SampleTestConfig.class)
                .run(context -> {
                    assertThat(context).hasBean("sampleValue");
                    assertThat(context.getBean("sampleValue", String.class))
                            .isEqualTo("test-value");
                });
    }

    @Test
    void applicationContextRunner_shouldSupportPropertyOverrides() {
        contextRunner
                .withPropertyValues("custom.property=hello")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getEnvironment().getProperty("custom.property"))
                            .isEqualTo("hello");
                });
    }

    @Test
    void applicationContextRunner_shouldReportMissingBeans() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(SampleTestConfig.class)
        );
    }

    @Configuration
    static class SampleTestConfig {
        @Bean
        String sampleValue() {
            return "test-value";
        }
    }
}
