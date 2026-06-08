package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the Spring Boot plugin enables ApplicationRunner and
 * CommandLineRunner support. These interfaces are invoked after the context
 * is ready (during bootRun) and are commonly used for startup tasks like
 * data seeding, cache warming, or connection validation.
 */
@SpringBootTest(
        classes = {EducationalPlatformApplication.class, SpringBootRunnerSupportTest.RunnerTestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class SpringBootRunnerSupportTest {

    static final List<String> executedRunners = new ArrayList<>();

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void applicationRunner_shouldBeInvokedOnStartup() {
        assertThat(executedRunners)
                .as("ApplicationRunner must be invoked during bootRun startup")
                .contains("applicationRunner");
    }

    @Test
    void commandLineRunner_shouldBeInvokedOnStartup() {
        assertThat(executedRunners)
                .as("CommandLineRunner must be invoked during bootRun startup")
                .contains("commandLineRunner");
    }

    @Test
    void runners_shouldExecuteAfterContextRefresh() {
        // Both runners should have completed by the time the test starts
        assertThat(executedRunners)
                .as("Both runners must have executed before test methods run")
                .hasSize(2);
    }

    @Test
    void applicationRunnerInterface_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.ApplicationRunner"))
                .as("ApplicationRunner interface must be available for startup tasks")
                .doesNotThrowAnyException();
    }

    @Test
    void commandLineRunnerInterface_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.CommandLineRunner"))
                .as("CommandLineRunner interface must be available for startup tasks")
                .doesNotThrowAnyException();
    }

    @TestConfiguration
    static class RunnerTestConfig {

        @Bean
        ApplicationRunner testApplicationRunner() {
            return args -> executedRunners.add("applicationRunner");
        }

        @Bean
        CommandLineRunner testCommandLineRunner() {
            return args -> executedRunners.add("commandLineRunner");
        }
    }
}
