package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that {@code @SpringBootTest(args = {...})} correctly passes
 * program arguments to the application context, simulating the way
 * {@code bootRun --args="..."} passes arguments at runtime. This is
 * distinct from the {@code properties} attribute which sets Spring
 * environment properties. The {@code args} attribute tests the same
 * code path as {@code SpringApplication.run(class, args)} in main().
 * <p>
 * No other test in the suite uses the {@code args} attribute of
 * {@code @SpringBootTest}; this test fills that gap to ensure the
 * Spring Boot test infrastructure properly delegates program arguments.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        args = {
                "--spring.application.name=args-test",
                "--custom.arg.key=from-args"
        }
)
class SpringBootTestArgsAttributeTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Test
    void argsAttribute_shouldPassProgramArgumentsToContext() {
        assertThat(environment.getProperty("custom.arg.key"))
                .as("Program argument --custom.arg.key must be resolvable in the Environment — "
                        + "@SpringBootTest(args = {...}) must delegate to the same argument "
                        + "processing path as SpringApplication.run(class, args)")
                .isEqualTo("from-args");
    }

    @Test
    void argsAttribute_shouldOverrideApplicationName() {
        assertThat(environment.getProperty("spring.application.name"))
                .as("--spring.application.name passed via args must override any default — "
                        + "program arguments have highest precedence in Spring property sources")
                .isEqualTo("args-test");
    }

    @Test
    void applicationContext_shouldBeActiveWithCustomArgs() {
        assertThat(applicationContext.isActive())
                .as("Application context must be fully active when started with custom args")
                .isTrue();
    }

    @Test
    void argsAttribute_shouldNotInterfereWithPropertySourceAnnotation() {
        assertThat(environment.containsProperty("com.educational.platform.security.enabled"))
                .as("@PropertySource(application-security.properties) must still be loaded "
                        + "even when custom args are passed — args do not disable @PropertySource")
                .isTrue();
    }
}
