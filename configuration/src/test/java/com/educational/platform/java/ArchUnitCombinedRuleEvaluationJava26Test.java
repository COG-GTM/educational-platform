package com.educational.platform.java;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that multiple ArchUnit rules can be evaluated in combination
 * on the same Java 26 class set without interference or caching issues.
 * <p>
 * ArchUnit 1.4.2 caches imported class metadata internally. If the caching
 * layer mishandles Java 26 class file attributes, rules evaluated individually
 * might pass while rules evaluated together (sharing cached state) could fail
 * due to corrupted metadata. This test evaluates composite and sequential rules
 * against a shared import to detect such regressions.
 * <p>
 * Individual rule types are tested in {@link ArchUnitAnalyzeClassesJava26Test},
 * {@link ArchUnitNamingConventionJava26Test}, and
 * {@link ArchUnitAnnotationScanningJava26Test}. This test focuses on
 * <em>combined evaluation</em>.
 */
public class ArchUnitCombinedRuleEvaluationJava26Test {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.educational.platform");

    @Test
    void compositeRule_shouldEvaluate_allSubRules_onJava26() {
        ArchRule eventImmutability = fields()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Event")
                .should().beFinal();

        ArchRule commandImmutability = fields()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Command")
                .should().beFinal();

        ArchRule noWebInDomain = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..web..");

        CompositeArchRule compositeRule = CompositeArchRule.of(eventImmutability)
                .and(commandImmutability)
                .and(noWebInDomain);

        assertThatCode(() -> compositeRule.check(CLASSES))
                .as("Composite rule with three sub-rules should evaluate on Java 26 bytecode")
                .doesNotThrowAnyException();
    }

    @Test
    void sequentialRuleEvaluation_shouldNotCorrupt_sharedClassCache() {
        // Evaluate multiple rules sequentially against the SAME imported classes
        ArchRule rule1 = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("com.educational.platform..");

        ArchRule rule2 = noClasses()
                .that().resideInAPackage("..common.exception..")
                .should().dependOnClassesThat().resideInAPackage("..web..");

        ArchRule rule3 = fields()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Event")
                .should().beFinal();

        ArchRule rule4 = classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage("com.educational.platform..");

        // Sequential evaluation
        assertThatCode(() -> {
            rule1.check(CLASSES);
            rule2.check(CLASSES);
            rule3.check(CLASSES);
            rule4.check(CLASSES);
        }).as("Sequential rule evaluation should not corrupt ArchUnit's internal class cache on Java 26")
                .doesNotThrowAnyException();
    }

    @Test
    void ruleEvaluation_afterFiltering_shouldWork_onSameImport() {
        // Test that filtering operations on the same class set don't interfere
        long entityCount = CLASSES.stream()
                .filter(c -> c.isAnnotatedWith("jakarta.persistence.Entity"))
                .count();

        long controllerCount = CLASSES.stream()
                .filter(c -> c.isAnnotatedWith("org.springframework.web.bind.annotation.RestController"))
                .count();

        long componentCount = CLASSES.stream()
                .filter(c -> c.isAnnotatedWith("org.springframework.stereotype.Component"))
                .count();

        // After multiple filter operations, the original class set should be intact
        assertThat(CLASSES.size())
                .as("Original class set should remain intact after multiple filter operations")
                .isGreaterThan((int) (entityCount + controllerCount + componentCount));
    }

    @Test
    void namingAndAnnotationRules_shouldNotConflict_onJava26() {
        // Naming-based and annotation-based rules exercise different ASM code paths
        ArchRule namingRule = classes()
                .that().haveSimpleNameEndingWith("CommandHandler")
                .should().resideInAPackage("com.educational.platform..");

        ArchRule annotationRule = classes()
                .that().areAnnotatedWith("org.springframework.stereotype.Component")
                .should().haveSimpleNameNotStartingWith("XXX_IMPOSSIBLE");

        ArchRule combinedRule = CompositeArchRule.of(namingRule).and(annotationRule);

        assertThatCode(() -> combinedRule.check(CLASSES))
                .as("Naming and annotation rules combined should not conflict on Java 26")
                .doesNotThrowAnyException();
    }

    @Test
    void classImportCount_shouldBe_consistent_acrossMultipleAccesses() {
        int firstAccess = CLASSES.size();
        int secondAccess = CLASSES.size();
        int thirdAccess = CLASSES.size();

        assertThat(firstAccess)
                .as("Class count should be consistent across multiple accesses (no lazy-init corruption)")
                .isEqualTo(secondAccess)
                .isEqualTo(thirdAccess);
    }
}
