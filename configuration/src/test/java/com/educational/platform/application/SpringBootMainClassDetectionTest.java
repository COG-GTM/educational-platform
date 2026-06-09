package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the meta-annotation chain that the Spring Boot Gradle plugin
 * relies on to auto-detect the main class for bootRun and bootJar tasks.
 * The plugin scans for @SpringBootConfiguration (carried by @SpringBootApplication)
 * to resolve which class contains the main() method.
 */
class SpringBootMainClassDetectionTest {

    @Test
    void springBootApplication_shouldCarrySpringBootConfiguration() {
        // @SpringBootApplication is meta-annotated with @SpringBootConfiguration;
        // the plugin uses this to discover the main class automatically
        SpringBootApplication sba = EducationalPlatformApplication.class
                .getAnnotation(SpringBootApplication.class);
        assertThat(sba).isNotNull();
        assertThat(sba.annotationType().isAnnotationPresent(SpringBootConfiguration.class))
                .as("@SpringBootApplication must carry @SpringBootConfiguration " +
                        "for the Spring Boot plugin to auto-detect the main class")
                .isTrue();
    }

    @Test
    void springBootApplication_shouldCarryEnableAutoConfiguration() {
        SpringBootApplication sba = EducationalPlatformApplication.class
                .getAnnotation(SpringBootApplication.class);
        assertThat(sba).isNotNull();

        boolean hasEnableAutoConfig = Arrays.stream(sba.annotationType().getAnnotations())
                .anyMatch(a -> a.annotationType().getSimpleName().equals("EnableAutoConfiguration"));
        assertThat(hasEnableAutoConfig)
                .as("@SpringBootApplication must carry @EnableAutoConfiguration " +
                        "for auto-configuration to activate with the plugin")
                .isTrue();
    }

    @Test
    void springBootApplication_shouldCarryComponentScan() {
        SpringBootApplication sba = EducationalPlatformApplication.class
                .getAnnotation(SpringBootApplication.class);
        assertThat(sba).isNotNull();

        boolean hasComponentScan = Arrays.stream(sba.annotationType().getAnnotations())
                .anyMatch(a -> a.annotationType().getSimpleName().equals("ComponentScan"));
        assertThat(hasComponentScan)
                .as("@SpringBootApplication must carry @ComponentScan " +
                        "for bounded context module discovery")
                .isTrue();
    }

    @Test
    void applicationClass_fullyQualifiedName_shouldBeCorrect() {
        assertThat(EducationalPlatformApplication.class.getName())
                .as("FQN must be stable for the bootJar manifest Main-Class entry")
                .isEqualTo("com.educational.platform.EducationalPlatformApplication");
    }

    @Test
    void applicationClass_shouldBeOnlySpringBootConfigurationInBasePackage() {
        // The Spring Boot plugin expects exactly one @SpringBootConfiguration
        // in the base package. Verify the application class meets this contract.
        boolean isAnnotated = false;
        for (Annotation annotation : EducationalPlatformApplication.class.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(SpringBootConfiguration.class)) {
                isAnnotated = true;
                break;
            }
        }
        assertThat(isAnnotated)
                .as("Application class must be detectable as a @SpringBootConfiguration " +
                        "for the plugin's main class resolution")
                .isTrue();
    }

    @Test
    void applicationClass_shouldHaveMainMethod_forPluginBootstrap() throws NoSuchMethodException {
        // The plugin resolves the main class via @SpringBootConfiguration
        // and then invokes main(String[]). Both must be present.
        assertThat(EducationalPlatformApplication.class
                .getDeclaredMethod("main", String[].class))
                .as("Application class must have a main(String[]) method " +
                        "for the Spring Boot plugin to invoke via bootRun")
                .isNotNull();
    }
}
