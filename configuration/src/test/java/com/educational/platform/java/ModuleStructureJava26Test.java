package com.educational.platform.java;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies that the modular monolith structure remains intact after the Java 26 upgrade.
 * <p>
 * ArchUnit 1.4.2 is required to parse Java 26 bytecode (class file major version 70).
 * These tests confirm that ArchUnit can analyze the full module graph and enforce
 * architectural constraints on all bounded contexts compiled with Java 26.
 */
public class ModuleStructureJava26Test {

    @ParameterizedTest(name = "ArchUnit should import classes from module: {0}")
    @ValueSource(strings = {
            "com.educational.platform.courses",
            "com.educational.platform.administration",
            "com.educational.platform.users",
            "com.educational.platform.course.enrollments",
            "com.educational.platform.course.reviews",
            "com.educational.platform.common"
    })
    void archUnit_shouldImport_eachBoundedContext(String modulePackage) {
        assertThatCode(() -> {
            JavaClasses classes = new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages(modulePackage);

            assertThat(classes)
                    .as("ArchUnit should import classes from %s (Java 26 bytecode)", modulePackage)
                    .isNotEmpty();
        }).doesNotThrowAnyException();
    }

    @Test
    void archUnit_shouldEnforce_domainIndependence_acrossAllModules() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.educational.platform");

        // Domain classes should not depend on web/infrastructure
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..web..");

        assertThatCode(() -> rule.check(classes))
                .as("Domain independence rule should pass on Java 26 compiled classes")
                .doesNotThrowAnyException();
    }

    @Test
    void archUnit_shouldEnforce_commonModule_hasNoDomainDependencies() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.educational.platform");

        ArchRule rule = noClasses()
                .that().resideInAPackage("..common..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..courses..",
                        "..administration..",
                        "..enrollments..",
                        "..reviews.."
                );

        assertThatCode(() -> rule.check(classes))
                .as("Common module should not depend on domain modules")
                .doesNotThrowAnyException();
    }

    @Test
    void archUnit_shouldDetect_totalClassCount_afterJava26Upgrade() {
        JavaClasses allClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.educational.platform");

        // The project has many classes across modules; ensure ArchUnit sees them all
        assertThat(allClasses.size())
                .as("ArchUnit should detect a significant number of production classes compiled with Java 26")
                .isGreaterThan(50);
    }

    @Test
    void archUnit_shouldEnforce_integrationEvents_areInCorrectPackage() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.educational.platform");

        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("IntegrationEvent")
                .should().resideInAPackage("..integration.events..")
                .orShould().resideInAPackage("..integration.event..");

        assertThatCode(() -> rule.check(classes))
                .as("Integration events should be in the correct package structure")
                .doesNotThrowAnyException();
    }

    @Test
    void archUnit_shouldParse_allModules_withoutBytecodeErrors() {
        // This is the critical test: if ArchUnit's ASM version doesn't support
        // class file major version 70 (Java 26), this import will fail
        assertThatCode(() -> {
            JavaClasses classes = new ClassFileImporter()
                    .importPackages("com.educational.platform");

            assertThat(classes.size())
                    .as("All classes (prod + test) across all modules should be importable")
                    .isGreaterThan(80);
        }).as("ArchUnit 1.4.2+ should handle all Java 26 classes without 'Unsupported class file major version 70'")
                .doesNotThrowAnyException();
    }
}
