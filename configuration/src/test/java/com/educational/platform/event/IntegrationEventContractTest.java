package com.educational.platform.event;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.educational.platform")
public class IntegrationEventContractTest {

    @ArchTest
    public static final ArchRule contractEvents_shouldBe_records = classes()
            .that().resideInAPackage("com.educational.platform.integration.event")
            .should().beRecords()
            .because("Shared integration event contracts should be immutable records.");

    @ArchTest
    public static final ArchRule contractModule_shouldNotDependOn_boundedContexts = noClasses()
            .that().resideInAPackage("com.educational.platform.integration.event")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.educational.platform.courses..",
                    "com.educational.platform.administration..",
                    "com.educational.platform.users..",
                    "com.educational.platform.course.enrollments..",
                    "com.educational.platform.course.reviews..")
            .because("The integration-events-contract module is owned by no bounded context and must stay neutral.");

    @ArchTest
    public static final ArchRule administration_shouldNotDependOn_coursesIntegrationEvents = noClasses()
            .that().resideInAPackage("com.educational.platform.administration..")
            .should().dependOnClassesThat().resideInAPackage("com.educational.platform.courses.integration.event..")
            .because("Administration must consume cross-context events via the neutral contract module, not courses' integration events.");

}
