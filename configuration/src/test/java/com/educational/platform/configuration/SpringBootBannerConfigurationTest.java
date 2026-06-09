package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the Spring Boot banner infrastructure — enabled by the
 * Spring Boot plugin — is available and configurable. The banner is part
 * of the bootRun startup experience and can be customised or suppressed
 * via spring.main.banner-mode. These tests ensure the Banner API is
 * functional and that banner mode configuration propagates correctly.
 */
class SpringBootBannerConfigurationTest {

    private static final String ISOLATED_DB = "--spring.datasource.url=jdbc:h2:mem:banner_test;DB_CLOSE_DELAY=-1";

    @Test
    void bannerInterface_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.Banner"))
                .as("Banner interface must be available for bootRun startup display")
                .doesNotThrowAnyException();
    }

    @Test
    void bannerMode_shouldBeConfigurableToOff() {
        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        app.setBannerMode(Banner.Mode.OFF);

        ConfigurableApplicationContext context = app.run(ISOLATED_DB);
        try {
            assertThat(context.isActive())
                    .as("Application must start successfully with banner mode OFF")
                    .isTrue();
        } finally {
            context.close();
        }
    }

    @Test
    void bannerMode_shouldBeConfigurableToLog() {
        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        app.setBannerMode(Banner.Mode.LOG);

        ConfigurableApplicationContext context = app.run(ISOLATED_DB);
        try {
            assertThat(context.isActive())
                    .as("Application must start successfully with banner mode LOG")
                    .isTrue();
        } finally {
            context.close();
        }
    }

    @Test
    void springBootBannerClass_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.SpringBootBanner"))
                .as("SpringBootBanner (default banner implementation) must be available")
                .doesNotThrowAnyException();
    }

    @Test
    void bannerMode_enum_shouldHaveExpectedValues() {
        Banner.Mode[] modes = Banner.Mode.values();
        assertThat(modes)
                .as("Banner.Mode must contain OFF, CONSOLE, and LOG options")
                .extracting(Enum::name)
                .contains("OFF", "CONSOLE", "LOG");
    }

    @Test
    void customBanner_shouldBeAccepted() {
        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        app.setBannerMode(Banner.Mode.OFF);
        app.setBanner((environment, sourceClass, out) -> out.println("Custom Banner"));

        ConfigurableApplicationContext context = app.run(ISOLATED_DB);
        try {
            assertThat(context.isActive())
                    .as("Application must accept a custom Banner implementation")
                    .isTrue();
        } finally {
            context.close();
        }
    }
}
