package com.educational.platform.java;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the three pre-existing ArchUnit tests still function correctly
 * after the ArchUnit 1.4.1 → 1.4.2 upgrade required for Java 26 bytecode support.
 * <p>
 * The original motivation for bumping ArchUnit was to avoid
 * "Unsupported class file major version 70" in {@code @AnalyzeClasses}-based tests.
 * This test programmatically re-evaluates the same architectural rules defined in
 * {@code IntegrationEventTest}, {@code CommandHandlerTest}, and {@code LayerTest}
 * to confirm they still pass against Java 26-compiled classes.
 */
public class ExistingArchUnitTestCompatibilityTest {

    private static final JavaClasses ALL_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.educational.platform");

    // --- IntegrationEventTest rule ---

    @Test
    void integrationEventImmutability_shouldPass_onJava26CompiledClasses() {
        ArchRule eventsImmutable = fields()
                .that()
                .areDeclaredInClassesThat()
                .haveSimpleNameEndingWith("Event")
                .should()
                .beFinal();

        assertThatCode(() -> eventsImmutable.check(ALL_CLASSES))
                .as("IntegrationEventTest's immutability rule should still pass with ArchUnit 1.4.2 on Java 26")
                .doesNotThrowAnyException();
    }

    // --- LayerTest rule ---

    @Test
    void controllersNotAccessingRepositories_shouldPass_onJava26CompiledClasses() {
        ArchRule controllersRule = noClasses()
                .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .should().accessClassesThat().areAssignableFrom(org.springframework.stereotype.Repository.class);

        assertThatCode(() -> controllersRule.check(ALL_CLASSES))
                .as("LayerTest's controller→repository rule should still pass with ArchUnit 1.4.2 on Java 26")
                .doesNotThrowAnyException();
    }

    // --- CommandHandlerTest rule ---

    @Test
    void commandImmutability_shouldPass_onJava26CompiledClasses() {
        ArchRule commandsImmutable = fields()
                .that()
                .areDeclaredInClassesThat()
                .haveSimpleNameEndingWith("Command")
                .should()
                .beFinal();

        assertThatCode(() -> commandsImmutable.check(ALL_CLASSES))
                .as("CommandHandlerTest's command immutability rule should still pass with ArchUnit 1.4.2 on Java 26")
                .doesNotThrowAnyException();
    }

    // --- Verify the test classes themselves are loadable ---

    @ParameterizedTest(name = "Existing ArchUnit test class should be importable by ArchUnit: {0}")
    @ValueSource(strings = {
            "com.educational.platform.event.IntegrationEventTest",
            "com.educational.platform.handler.CommandHandlerTest",
            "com.educational.platform.layer.LayerTest"
    })
    void existingTestClass_shouldBe_importableByArchUnit(String className) {
        assertThatCode(() -> {
            Class<?> testClass = Class.forName(className);
            JavaClasses imported = new ClassFileImporter().importClasses(testClass);
            assertThat(imported).isNotEmpty();
        }).as("ArchUnit 1.4.2 should import existing test class '%s' compiled with Java 26", className)
                .doesNotThrowAnyException();
    }

    @Test
    void archUnit_shouldImport_allProductionClasses_fromEveryModule() {
        assertThat(ALL_CLASSES.size())
                .as("ArchUnit 1.4.2 should import all production classes across bounded contexts (Java 26)")
                .isGreaterThan(50);
    }
}
