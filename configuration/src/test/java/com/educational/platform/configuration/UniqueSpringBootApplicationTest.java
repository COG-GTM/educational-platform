package com.educational.platform.configuration;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Guards against multiple @SpringBootApplication or @EnableAsync entry points.
 * The Spring Boot plugin requires exactly one @SpringBootApplication-annotated
 * class for main class auto-detection; a second would cause ambiguous resolution
 * and fail bootRun/bootJar.
 */
@AnalyzeClasses(packages = "com.educational.platform")
public class UniqueSpringBootApplicationTest {

    @ArchTest
    public static final ArchRule onlyOneClass_shouldHaveSpringBootApplication = classes()
            .that().areAnnotatedWith(SpringBootApplication.class)
            .should().haveSimpleName("EducationalPlatformApplication")
            .because("The Spring Boot plugin requires exactly one @SpringBootApplication class "
                    + "for auto-detection of the main class; adding a second would break bootRun/bootJar.");

    @ArchTest
    public static final ArchRule onlyOneClass_shouldHaveEnableAsync = classes()
            .that().areAnnotatedWith(EnableAsync.class)
            .should().haveSimpleName("EducationalPlatformApplication")
            .because("@EnableAsync must only be declared on the application entry point "
                    + "to ensure a single async task executor configuration for inter-module events.");
}
