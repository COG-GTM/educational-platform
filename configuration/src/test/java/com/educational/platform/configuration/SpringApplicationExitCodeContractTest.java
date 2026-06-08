package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates the application's exit code contract through SpringApplication.exit().
 * The Spring Boot plugin's bootRun task uses the JVM process exit code to signal
 * success or failure to the Gradle daemon. If exit codes are swallowed or
 * incorrectly mapped, CI pipelines and orchestration tools cannot detect
 * application failures.
 * <p>
 * Complements {@link MainMethodErrorPropagationTest} and
 * {@link MainMethodExceptionPropagationTest} (exception propagation through main())
 * by testing the structured exit-code mechanism that Spring Boot provides as an
 * alternative to exception-based failure signaling.
 */
class SpringApplicationExitCodeContractTest {

    private static final String ISOLATED_DB =
            "--spring.datasource.url=jdbc:h2:mem:exitcode_test;DB_CLOSE_DELAY=-1";

    @Test
    void exitCode_shouldBeZero_forSuccessfulShutdown() {
        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext context = app.run(ISOLATED_DB);

        int exitCode = SpringApplication.exit(context);
        assertThat(exitCode)
                .as("Exit code must be 0 for a clean shutdown — "
                        + "non-zero would signal failure to bootRun/Gradle/CI")
                .isEqualTo(0);
    }

    @Test
    void exitCode_shouldReflectCustomExitCodeGenerator() {
        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext context = app.run(ISOLATED_DB);

        ExitCodeGenerator generator = () -> 42;
        int exitCode = SpringApplication.exit(context, generator);
        assertThat(exitCode)
                .as("Exit code must reflect the ExitCodeGenerator value — "
                        + "this mechanism is used for signaling specific failure "
                        + "modes to orchestration tools via bootRun")
                .isEqualTo(42);
    }

    @Test
    void exitCodeGeneratorInterface_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.ExitCodeGenerator"))
                .as("ExitCodeGenerator must be available for structured shutdown signaling")
                .doesNotThrowAnyException();
    }

    @Test
    void context_shouldBeClosed_afterExit() {
        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext context = app.run(ISOLATED_DB);

        SpringApplication.exit(context);
        assertThat(context.isActive())
                .as("Application context must be inactive after SpringApplication.exit()")
                .isFalse();
    }

    @Test
    void exit_withMultipleGenerators_shouldReturnNonZeroExitCode() {
        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext context = app.run(ISOLATED_DB);

        ExitCodeGenerator gen1 = () -> 0;
        ExitCodeGenerator gen2 = () -> 5;
        int exitCode = SpringApplication.exit(context, gen1, gen2);
        assertThat(exitCode)
                .as("With multiple ExitCodeGenerators, a non-zero code must be returned "
                        + "to signal failure to bootRun/Gradle/CI")
                .isNotZero();
    }
}
