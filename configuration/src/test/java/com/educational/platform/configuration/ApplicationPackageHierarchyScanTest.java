package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the application class's package placement enables
 * @SpringBootApplication's default component scanning to discover all
 * bounded context modules. The Spring Boot plugin's bootRun task starts
 * the application from {@link EducationalPlatformApplication}; if this
 * class is not in a parent package of all module packages, component
 * scanning would silently miss beans and the application would start
 * with missing dependencies.
 *
 * Complements {@link EducationalPlatformApplicationMainTest} (which
 * validates the package name is "com.educational.platform") by verifying
 * that bounded context packages are actually discoverable beneath it.
 */
class ApplicationPackageHierarchyScanTest {

    private static final String BASE_PACKAGE = EducationalPlatformApplication.class.getPackageName();

    @Test
    void applicationClass_packageShouldBeBasePackage() {
        assertThat(BASE_PACKAGE)
                .as("Application class must be in 'com.educational.platform' base package")
                .isEqualTo("com.educational.platform");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "com.educational.platform.courses",
            "com.educational.platform.users",
            "com.educational.platform.administration",
            "com.educational.platform.security"
    })
    void boundedContextPackages_shouldBeUnderBasePackage(String modulePackage) {
        assertThat(modulePackage)
                .as("Bounded context package '%s' must start with the application's base package '%s' "
                        + "so that @SpringBootApplication's default component scanning discovers it",
                        modulePackage, BASE_PACKAGE)
                .startsWith(BASE_PACKAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "com.educational.platform.courses.course.Course",
            "com.educational.platform.users.User",
            "com.educational.platform.administration.course.approve.ApproveCourseProposalCommand"
    })
    void domainClasses_shouldBeResolvableFromConfigurationModule(String className) {
        assertThatCode(() -> Class.forName(className))
                .as("Domain class '%s' must be resolvable from the configuration module classpath — "
                        + "bootRun/bootJar needs all bounded context classes available", className)
                .doesNotThrowAnyException();
    }

    @Test
    void springBootApplication_shouldUseDefaultScanBasePackages() {
        SpringBootApplication sba = EducationalPlatformApplication.class
                .getAnnotation(SpringBootApplication.class);
        assertThat(sba).isNotNull();
        assertThat(sba.scanBasePackages())
                .as("@SpringBootApplication must rely on default scanning from its own package — "
                        + "explicit scanBasePackages would narrow the scan and break modular discovery")
                .isEmpty();
    }

    @Test
    void basePackage_shouldNotBeRootPackage() {
        assertThat(BASE_PACKAGE)
                .as("Application class must not be in the root/default package — "
                        + "root-package scanning causes severe startup performance issues "
                        + "and conflicts with JDK internal classes")
                .isNotEmpty()
                .contains(".");
    }

    @Test
    void basePackage_shouldHaveExactlyThreeSegments() {
        long segmentCount = BASE_PACKAGE.chars().filter(c -> c == '.').count() + 1;
        assertThat(segmentCount)
                .as("Base package 'com.educational.platform' should have exactly 3 segments — "
                        + "deeper nesting would exclude sibling packages from component scanning")
                .isEqualTo(3);
    }
}
