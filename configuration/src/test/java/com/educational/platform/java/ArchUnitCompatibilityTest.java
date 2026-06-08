package com.educational.platform.java;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that ArchUnit 1.4.2 can parse all project classes compiled with Java 26
 * (class file major version 70).
 * <p>
 * Prior to 1.4.2, ArchUnit's bundled ASM could not handle major version 70,
 * failing with "Unsupported class file major version 70".
 */
public class ArchUnitCompatibilityTest {

    @Test
    void archUnit_shouldParse_allProductionClasses_compiledWithJava26() {
        assertThatCode(() -> {
            JavaClasses classes = new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.educational.platform");

            assertThat(classes)
                    .as("ArchUnit should import production classes without bytecode errors")
                    .isNotEmpty();
        }).doesNotThrowAnyException();
    }

    @Test
    void archUnit_shouldParse_testClasses_compiledWithJava26() {
        assertThatCode(() -> {
            JavaClasses classes = new ClassFileImporter()
                    .importPackages("com.educational.platform");

            assertThat(classes)
                    .as("ArchUnit should import all classes (including tests) without bytecode errors")
                    .isNotEmpty();
        }).doesNotThrowAnyException();
    }

    @Test
    void archUnit_shouldReportCorrectNumberOfClasses() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.educational.platform");

        // Sanity check: the project has a non-trivial number of classes
        assertThat(classes.size())
                .as("Project should have a meaningful number of importable classes")
                .isGreaterThan(10);
    }

    @Test
    void archUnit_shouldEvaluateRules_againstJava26CompiledClasses() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.educational.platform");

        ArchRule rule = noClasses()
                .that().resideInAPackage("..common.exception..")
                .should().dependOnClassesThat().resideInAPackage("..web..");

        assertThatCode(() -> rule.check(classes))
                .as("ArchUnit should evaluate architectural rules on Java 26 classes without errors")
                .doesNotThrowAnyException();
    }

    @Test
    void archUnit_shouldDistinguish_productionAndTestClasses() {
        JavaClasses prodClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.educational.platform");

        JavaClasses allClasses = new ClassFileImporter()
                .importPackages("com.educational.platform");

        assertThat(allClasses.size())
                .as("All classes (including tests) should be more than production classes alone")
                .isGreaterThan(prodClasses.size());
    }

    @Test
    void archUnit_shouldImport_itsOwnTestClass_compiledWithJava26() {
        assertThatCode(() -> {
            JavaClasses classes = new ClassFileImporter().importClasses(ArchUnitCompatibilityTest.class);
            assertThat(classes).isNotEmpty();
        })
                .as("ArchUnit should be able to import its own test class compiled with Java 26")
                .doesNotThrowAnyException();
    }

    @Test
    void archUnit_shouldParse_classesFromMultipleBoundedContexts() {
        assertThatCode(() -> {
            JavaClasses classes = new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages(
                            "com.educational.platform.courses",
                            "com.educational.platform.administration",
                            "com.educational.platform.users"
                    );

            assertThat(classes)
                    .as("ArchUnit should import classes from multiple bounded contexts without bytecode errors")
                    .isNotEmpty();
        }).doesNotThrowAnyException();
    }

    @Test
    void archUnit_shouldEvaluate_layerRule_onJava26Classes() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.educational.platform");

        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

        assertThatCode(() -> rule.check(classes))
                .as("ArchUnit should evaluate layer dependency rules on Java 26 classes")
                .doesNotThrowAnyException();
    }
}
