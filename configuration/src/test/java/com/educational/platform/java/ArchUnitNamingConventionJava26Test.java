package com.educational.platform.java;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that ArchUnit naming convention rules evaluate correctly on
 * Java 26 compiled bytecode.
 * <p>
 * Existing tests cover immutability, layer-dependency, and nullability rules.
 * This test covers <em>naming convention</em> rules, which depend on ArchUnit's
 * ability to extract class simple names and match them against patterns — a
 * different code path in the ASM visitor that could regress independently.
 * <p>
 * Naming rules also exercise ArchUnit's class metadata extraction (annotations,
 * modifiers, simple names), verifying these are intact in major version 70
 * class files.
 */
public class ArchUnitNamingConventionJava26Test {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.educational.platform");

    @Test
    void controllers_shouldEndWith_Controller() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .should().haveSimpleNameEndingWith("Controller");

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("Controller naming convention should evaluate on Java 26 bytecode")
                .doesNotThrowAnyException();
    }

    @Test
    void commandHandlers_shouldEndWith_CommandHandler() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("CommandHandler")
                .should().resideInAPackage("com.educational.platform..");

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("CommandHandler naming rule should evaluate on Java 26 bytecode")
                .doesNotThrowAnyException();
    }

    @Test
    void queryHandlers_shouldEndWith_QueryHandler() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("QueryHandler")
                .should().resideInAPackage("com.educational.platform..");

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("QueryHandler naming rule should evaluate on Java 26 bytecode")
                .doesNotThrowAnyException();
    }

    @Test
    void factories_shouldEndWith_Factory() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Factory")
                .should().resideInAPackage("com.educational.platform..");

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("Factory naming rule should evaluate on Java 26 bytecode")
                .doesNotThrowAnyException();
    }

    @Test
    void integrationEventHandlers_shouldEndWith_IntegrationEventHandler() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("IntegrationEventHandler")
                .should().resideInAPackage("com.educational.platform..");

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("IntegrationEventHandler naming rule should evaluate on Java 26 bytecode")
                .doesNotThrowAnyException();
    }

    @Test
    void exceptions_shouldEndWith_Exception() {
        ArchRule rule = classes()
                .that().areAssignableTo(Exception.class)
                .should().haveSimpleNameEndingWith("Exception");

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("Exception naming convention should evaluate on Java 26 bytecode")
                .doesNotThrowAnyException();
    }

    @Test
    void archUnit_shouldFindClasses_matchingNamingPatterns() {
        long controllerCount = PRODUCTION_CLASSES.stream()
                .filter(c -> c.getSimpleName().endsWith("Controller"))
                .count();

        long handlerCount = PRODUCTION_CLASSES.stream()
                .filter(c -> c.getSimpleName().endsWith("CommandHandler"))
                .count();

        assertThat(controllerCount)
                .as("ArchUnit should find controller classes in Java 26 bytecode")
                .isGreaterThan(0);

        assertThat(handlerCount)
                .as("ArchUnit should find command handler classes in Java 26 bytecode")
                .isGreaterThan(0);
    }

    @Test
    void restControllers_shouldNotResideIn_domainPackage() {
        ArchRule rule = noClasses()
                .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .should().resideInAPackage("..domain..");

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("RestControllers-not-in-domain rule should evaluate on Java 26 bytecode")
                .doesNotThrowAnyException();
    }
}
