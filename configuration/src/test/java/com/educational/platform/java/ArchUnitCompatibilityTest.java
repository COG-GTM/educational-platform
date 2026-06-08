package com.educational.platform.java;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

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
}
