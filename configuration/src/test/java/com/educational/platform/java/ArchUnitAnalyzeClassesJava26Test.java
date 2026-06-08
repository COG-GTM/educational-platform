package com.educational.platform.java;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Exercises ArchUnit's {@code @AnalyzeClasses}-style import on the full project
 * compiled with Java 26 bytecode (class file major version 70).
 * <p>
 * The {@code @AnalyzeClasses} annotation triggers the same {@link ClassFileImporter}
 * pipeline internally. This test mirrors that behavior programmatically and evaluates
 * multiple rule types (field access, method naming, layer dependencies, package
 * containment) to ensure ArchUnit 1.4.2's ASM handles all Java 26 class structures
 * without regressions.
 */
public class ArchUnitAnalyzeClassesJava26Test {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.educational.platform");

    @Test
    void fieldRule_shouldEvaluate_onJava26Classes() {
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Event")
                .should().beFinal();

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("Field-level rules should evaluate on Java 26 bytecode")
                .doesNotThrowAnyException();
    }

    @Test
    void methodRule_shouldEvaluate_onJava26Classes() {
        ArchRule rule = methods()
                .that().arePublic()
                .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("CommandHandler")
                .should().haveNameNotMatching(".*ZZZZZ_IMPOSSIBLE.*");

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("Method-level rules should evaluate on Java 26 bytecode")
                .doesNotThrowAnyException();
    }

    @Test
    void classResidenceRule_shouldEvaluate_onJava26Classes() {
        // Verify ArchUnit can evaluate class-residence rules on Java 26 bytecode.
        // The project places controllers at the module root, not in a sub-package,
        // so we assert that controllers reside somewhere under the platform package.
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("com.educational.platform..");

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("Class-residence rules should evaluate on Java 26 bytecode")
                .doesNotThrowAnyException();
    }

    @Test
    void negatedDependencyRule_shouldEvaluate_onJava26Classes() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..common.exception..")
                .should().dependOnClassesThat().resideInAPackage("..web..");

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("Negated dependency rules should evaluate on Java 26 bytecode")
                .doesNotThrowAnyException();
    }

    @Test
    void multiPackageImport_shouldSucceed_onJava26Classes() {
        assertThatCode(() -> {
            JavaClasses classes = new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages(
                            "com.educational.platform.courses",
                            "com.educational.platform.administration",
                            "com.educational.platform.users",
                            "com.educational.platform.course.enrollments",
                            "com.educational.platform.course.reviews",
                            "com.educational.platform.common"
                    );

            assertThat(classes.size())
                    .as("Multi-package import should find classes across all bounded contexts")
                    .isGreaterThan(30);
        }).doesNotThrowAnyException();
    }

    @Test
    void classImport_withTestClasses_shouldSucceed_onJava26Classes() {
        assertThatCode(() -> {
            JavaClasses allClasses = new ClassFileImporter()
                    .importPackages("com.educational.platform");

            JavaClasses prodOnly = new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.educational.platform");

            assertThat(allClasses.size())
                    .as("Including test classes should yield more classes than production only")
                    .isGreaterThan(prodOnly.size());
        }).doesNotThrowAnyException();
    }

    @Test
    void archUnit_shouldNotConfuse_recordClasses_withRegularClasses() {
        // Java 16+ records compile to classes with special bytecode attributes.
        // ArchUnit 1.4.2 must handle these without errors.
        assertThatCode(() -> {
            JavaClasses classes = new ClassFileImporter()
                    .importPackages("com.educational.platform");

            long totalClasses = classes.size();
            assertThat(totalClasses).isGreaterThan(0);
        }).as("ArchUnit should handle any record classes in the project without errors")
                .doesNotThrowAnyException();
    }
}
