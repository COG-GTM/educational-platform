package com.educational.platform.java;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that ArchUnit correctly scans and evaluates annotation-based rules
 * on Java 26 compiled bytecode.
 * <p>
 * Unlike {@link ArchUnitAnalyzeClassesJava26Test} (which tests rule types) and
 * {@link ExistingArchUnitTestCompatibilityTest} (which re-evaluates pre-existing
 * rules), this test focuses on ArchUnit's <em>annotation scanning</em> code path:
 * detecting {@code @RestController}, {@code @Entity}, {@code @Component}, and
 * similar annotations on classes compiled to class file major version 70.
 * <p>
 * ArchUnit delegates annotation reading to ASM visitors. If the ASM version
 * bundled with ArchUnit 1.4.2 mishandles annotation attributes in Java 26
 * class files, these rules would fail to match annotated classes and silently
 * produce incorrect results.
 */
public class ArchUnitAnnotationScanningJava26Test {

    private static final String ENTITY_ANNOTATION = "jakarta.persistence.Entity";
    private static final String ID_ANNOTATION = "jakarta.persistence.Id";
    private static final String REST_CONTROLLER_ANNOTATION = "org.springframework.web.bind.annotation.RestController";
    private static final String COMPONENT_ANNOTATION = "org.springframework.stereotype.Component";

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.educational.platform");

    @Test
    void archUnit_shouldDetect_restControllerAnnotation_onJava26Classes() {
        long count = PRODUCTION_CLASSES.stream()
                .filter(c -> c.isAnnotatedWith(REST_CONTROLLER_ANNOTATION))
                .count();

        assertThat(count)
                .as("ArchUnit should detect @RestController annotations on Java 26 classes")
                .isGreaterThan(0);
    }

    @Test
    void archUnit_shouldDetect_entityAnnotation_onJava26Classes() {
        long count = PRODUCTION_CLASSES.stream()
                .filter(c -> c.isAnnotatedWith(ENTITY_ANNOTATION))
                .count();

        assertThat(count)
                .as("ArchUnit should detect @Entity annotations on Java 26 classes")
                .isGreaterThan(0);
    }

    @Test
    void archUnit_shouldDetect_componentAnnotation_onJava26Classes() {
        long count = PRODUCTION_CLASSES.stream()
                .filter(c -> c.isAnnotatedWith(COMPONENT_ANNOTATION))
                .count();

        assertThat(count)
                .as("ArchUnit should detect @Component annotations on Java 26 classes")
                .isGreaterThan(0);
    }

    @Test
    void archUnit_shouldEvaluateRule_basedOnEntityAnnotation() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(ENTITY_ANNOTATION)
                .should().resideInAPackage("com.educational.platform..");

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("@Entity-based rule should evaluate on Java 26 bytecode")
                .doesNotThrowAnyException();
    }

    @Test
    void archUnit_shouldEvaluateRule_basedOnComponentAnnotation() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(COMPONENT_ANNOTATION)
                .should().haveSimpleNameNotStartingWith("XXX_IMPOSSIBLE");

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("@Component-based rule should evaluate on Java 26 bytecode")
                .doesNotThrowAnyException();
    }

    @Test
    void archUnit_entityClasses_shouldNotDependOnWebLayer() {
        ArchRule rule = noClasses()
                .that().areAnnotatedWith(ENTITY_ANNOTATION)
                .should().dependOnClassesThat()
                .areAnnotatedWith(REST_CONTROLLER_ANNOTATION);

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("Entity→Controller dependency rule based on annotations should evaluate on Java 26")
                .doesNotThrowAnyException();
    }

    @Test
    void archUnit_entityFields_shouldNotBeStatic_forIdFields() {
        ArchRule rule = fields()
                .that().areAnnotatedWith(ID_ANNOTATION)
                .should().notBeStatic();

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("@Id field annotation rule should evaluate on Java 26 bytecode")
                .doesNotThrowAnyException();
    }

    @Test
    void archUnit_shouldMatchAnnotatedAndUnannotatedClasses_distinctly() {
        long entityClasses = PRODUCTION_CLASSES.stream()
                .filter(c -> c.isAnnotatedWith(ENTITY_ANNOTATION))
                .count();

        long nonEntityClasses = PRODUCTION_CLASSES.stream()
                .filter(c -> !c.isAnnotatedWith(ENTITY_ANNOTATION))
                .count();

        assertThat(entityClasses)
                .as("Should find entity classes")
                .isGreaterThan(0);

        assertThat(nonEntityClasses)
                .as("Should find non-entity classes")
                .isGreaterThan(entityClasses);
    }

    // --- @AnalyzeClasses annotation-driven import path ---

    @AnalyzeClasses(packages = "com.educational.platform")
    static class AnnotationDrivenImportValidation {

        @ArchTest
        static final ArchRule entities_shouldResideInPlatformPackage = classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage("com.educational.platform..")
                .because("Entities discovered via @AnalyzeClasses should reside in platform packages on Java 26");
    }

    @Test
    void analyzeClassesAnnotation_shouldBe_presentOnInnerClass() {
        assertThat(AnnotationDrivenImportValidation.class.isAnnotationPresent(AnalyzeClasses.class))
                .as("@AnalyzeClasses annotation should be retained at runtime on Java 26 compiled inner class")
                .isTrue();

        AnalyzeClasses annotation = AnnotationDrivenImportValidation.class.getAnnotation(AnalyzeClasses.class);
        assertThat(annotation.packages())
                .as("@AnalyzeClasses packages attribute should be accessible")
                .contains("com.educational.platform");
    }
}
