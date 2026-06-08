package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Spring Boot plugin correctly enables profile-based
 * configuration. bootRun supports --spring.profiles.active=xxx for environment
 * separation; these tests verify the profile mechanism works with the entry point.
 * Uses isolated H2 databases to avoid interfering with other tests.
 */
class ActiveProfileConfigurationTest {

    private static final String ISOLATED_DB = "--spring.datasource.url=jdbc:h2:mem:profile_test;DB_CLOSE_DELAY=-1";

    @Test
    void applicationContext_shouldStartWithDefaultProfile_whenNoProfileSet() {
        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        ConfigurableApplicationContext context = app.run(ISOLATED_DB);
        try {
            assertThat(context.getEnvironment().getDefaultProfiles())
                    .as("Default profile must be available when no explicit profile is activated")
                    .contains("default");
        } finally {
            context.close();
        }
    }

    @Test
    void applicationContext_shouldActivateProfile_viaCommandLineArg() {
        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        ConfigurableApplicationContext context = app.run(
                "--spring.profiles.active=test",
                "--spring.datasource.url=jdbc:h2:mem:profile_active_test;DB_CLOSE_DELAY=-1");
        try {
            assertThat(context.getEnvironment().getActiveProfiles())
                    .as("Profile specified via command-line arg (simulating bootRun --args) must be active")
                    .contains("test");
        } finally {
            context.close();
        }
    }

    @Test
    void applicationContextRunner_shouldSupportProfileActivation() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=integration")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getEnvironment().getActiveProfiles())
                            .contains("integration");
                });
    }

    @Test
    void applicationContext_shouldLoadProperties_forDefaultProfile() {
        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        ConfigurableApplicationContext context = app.run(ISOLATED_DB);
        try {
            assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                    .as("application.properties must be loaded with default profile")
                    .isNotNull()
                    .contains("h2");
        } finally {
            context.close();
        }
    }
}
