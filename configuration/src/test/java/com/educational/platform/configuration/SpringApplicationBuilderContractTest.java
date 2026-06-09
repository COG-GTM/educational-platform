package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the application entry point works with SpringApplicationBuilder,
 * an alternative to the direct SpringApplication.run() call used in main().
 * The builder pattern is used in production for hierarchical contexts, test
 * scenarios, and IDE-based launches. These tests ensure the application class
 * remains compatible with both startup paths after the Spring Boot plugin was
 * applied.
 * <p>
 * Complements {@link ActiveProfileConfigurationTest} (profile activation via
 * direct SpringApplication) and {@link SpringBootBannerConfigurationTest}
 * (banner configuration via direct SpringApplication). This test covers the
 * builder path which uses a different internal code flow.
 */
class SpringApplicationBuilderContractTest {

    private static final String ISOLATED_DB =
            "--spring.datasource.url=jdbc:h2:mem:builder_test;DB_CLOSE_DELAY=-1";

    @Test
    void applicationBuilder_shouldBootstrapContextSuccessfully() {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(
                EducationalPlatformApplication.class)
                .web(WebApplicationType.NONE)
                .run(ISOLATED_DB);
        try {
            assertThat(context.isActive())
                    .as("SpringApplicationBuilder must successfully bootstrap the application — "
                            + "this alternative to SpringApplication.run() is used by IDEs "
                            + "and test frameworks")
                    .isTrue();
        } finally {
            context.close();
        }
    }

    @Test
    void applicationBuilder_shouldResolveSameApplicationClass() {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(
                EducationalPlatformApplication.class)
                .web(WebApplicationType.NONE)
                .run(ISOLATED_DB);
        try {
            assertThat(context.containsBean("educationalPlatformApplication"))
                    .as("The application class bean must be registered when using "
                            + "SpringApplicationBuilder, same as with SpringApplication.run()")
                    .isTrue();
        } finally {
            context.close();
        }
    }

    @Test
    void applicationBuilder_shouldSupportProfileActivation() {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(
                EducationalPlatformApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("test")
                .run("--spring.datasource.url=jdbc:h2:mem:builder_profile_test;DB_CLOSE_DELAY=-1");
        try {
            assertThat(context.getEnvironment().getActiveProfiles())
                    .as("Profiles set via builder must be active in the resulting context")
                    .contains("test");
        } finally {
            context.close();
        }
    }

    @Test
    void applicationBuilder_shouldSupportPropertyOverrides() {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(
                EducationalPlatformApplication.class)
                .web(WebApplicationType.NONE)
                .properties("custom.test.key=builder-value")
                .run("--spring.datasource.url=jdbc:h2:mem:builder_props_test;DB_CLOSE_DELAY=-1");
        try {
            assertThat(context.getEnvironment().getProperty("custom.test.key"))
                    .as("Properties set via builder must be resolvable in the context")
                    .isEqualTo("builder-value");
        } finally {
            context.close();
        }
    }

    @Test
    void springApplicationBuilder_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.builder.SpringApplicationBuilder"))
                .as("SpringApplicationBuilder must be available — it is part of spring-boot "
                        + "and required for builder-style application startup")
                .doesNotThrowAnyException();
    }
}
