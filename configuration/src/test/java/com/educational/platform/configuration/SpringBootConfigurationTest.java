package com.educational.platform.configuration;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Validates architectural constraints for the Spring Boot entry point.
 * The Spring Boot plugin requires a properly annotated application class
 * to function; these rules guard against accidental removal or misconfiguration.
 */
@AnalyzeClasses(packages = "com.educational.platform")
public class SpringBootConfigurationTest {

    @ArchTest
    public static final ArchRule applicationEntryPoint_shouldBeAnnotated_withSpringBootApplication = classes()
            .that().haveSimpleNameEndingWith("PlatformApplication")
            .should().beAnnotatedWith(SpringBootApplication.class)
            .because("The configuration module entry point must be annotated with @SpringBootApplication "
                    + "for the Spring Boot plugin (bootRun, bootJar) to work.");

    @ArchTest
    public static final ArchRule applicationEntryPoint_shouldBeAnnotated_withEnableAsync = classes()
            .that().haveSimpleNameEndingWith("PlatformApplication")
            .should().beAnnotatedWith(EnableAsync.class)
            .because("Async event publishing between bounded contexts requires @EnableAsync on the application entry point.");

    @ArchTest
    public static final ArchRule applicationEntryPoint_shouldBeAnnotated_withPropertySource = classes()
            .that().haveSimpleNameEndingWith("PlatformApplication")
            .should().beAnnotatedWith(PropertySource.class)
            .because("The application entry point must load security configuration via @PropertySource "
                    + "for Spring Security to function correctly with bootRun.");

    @ArchTest
    public static final ArchRule applicationEntryPoint_shouldBePublic = classes()
            .that().haveSimpleNameEndingWith("PlatformApplication")
            .should().bePublic()
            .because("The application class must be public for the Spring Boot plugin to instantiate it.");

    @ArchTest
    public static final ArchRule applicationEntryPoint_shouldNotBeInterfaces = classes()
            .that().haveSimpleNameEndingWith("PlatformApplication")
            .should().notBeInterfaces()
            .because("The application entry point must be a concrete class, not an interface.");

    @ArchTest
    public static final ArchRule applicationEntryPoint_shouldResideInBasePackage = classes()
            .that().haveSimpleNameEndingWith("PlatformApplication")
            .should().resideInAPackage("com.educational.platform")
            .because("The application class must reside in the base package for component scanning "
                    + "to discover all bounded context modules.");
}
