package com.educational.platform.java;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the CommandHandlerTest nullability annotation rule still
 * passes after the ArchUnit 1.4.1 to 1.4.2 upgrade for Java 26 support.
 * <p>
 * {@code ExistingArchUnitTestCompatibilityTest} covers the immutability rules
 * from IntegrationEventTest, LayerTest, and CommandHandlerTest. However, it
 * does not cover the {@code commandHandlers_shouldHave_nullabilityAnnotations}
 * rule, which uses a {@link DescribedPredicate} on method return types and is
 * the most complex pre-existing ArchUnit rule in the project. This test fills
 * that gap.
 */
public class CommandHandlerNullabilityRuleJava26Test {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.educational.platform");

    @Test
    void nullabilityAnnotationRule_shouldPass_onJava26CompiledClasses() {
        ArchRule rule = methods()
                .that().arePublic()
                .and().doNotHaveRawReturnType(new DescribedPredicate<>("native or void") {
                    @Override
                    public boolean test(JavaClass javaClass) {
                        return javaClass.isPrimitive() || javaClass.isEquivalentTo(Void.TYPE);
                    }
                })
                .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("CommandHandler")
                .should().beAnnotatedWith(Nonnull.class)
                .orShould().beAnnotatedWith(Nullable.class);

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("CommandHandlerTest's nullability rule should pass with ArchUnit 1.4.2 on Java 26")
                .doesNotThrowAnyException();
    }

    @Test
    void describedPredicate_shouldEvaluate_onJava26BytecodeWithoutErrors() {
        // The DescribedPredicate accesses JavaClass metadata (isPrimitive,
        // isEquivalentTo) which depend on correct ASM parsing of Java 26 bytecode.
        ArchRule rule = methods()
                .that().arePublic()
                .and().doNotHaveRawReturnType(new DescribedPredicate<>("void only") {
                    @Override
                    public boolean test(JavaClass javaClass) {
                        return javaClass.isEquivalentTo(Void.TYPE);
                    }
                })
                .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("CommandHandler")
                .should().notBeAnnotatedWith(Deprecated.class);

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("DescribedPredicate with isEquivalentTo should work on Java 26 classes")
                .doesNotThrowAnyException();
    }

    @Test
    void methodLevelAnalysis_shouldWork_onJava26CompiledClasses() {
        // Validates that ArchUnit 1.4.2's method-level analysis (used by
        // CommandHandlerTest's DescribedPredicate) handles Java 26 bytecode
        // method descriptors correctly.
        ArchRule rule = methods()
                .that().arePublic()
                .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("CommandHandler")
                .should().haveNameNotMatching(".*ZZZZZ_IMPOSSIBLE.*");

        assertThatCode(() -> rule.check(PRODUCTION_CLASSES))
                .as("Method-level analysis of CommandHandler classes should work on Java 26")
                .doesNotThrowAnyException();
    }
}
