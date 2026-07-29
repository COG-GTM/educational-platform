package com.educational.platform.event;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.educational.platform")
public class ContractsIntegrationEventTest {

	@ArchTest
	public static final ArchRule contracts_events_shouldBe_records = classes()
			.that()
			.resideInAPackage("com.educational.platform.contracts.event..")
			.should()
			.beAssignableTo(Record.class)
			.because("Cross-context integration events are immutable shared contracts.");

	@ArchTest
	public static final ArchRule contracts_shouldNotDependOn_boundedContexts = noClasses()
			.that()
			.resideInAPackage("com.educational.platform.contracts..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage(
					"com.educational.platform.administration..",
					"com.educational.platform.courses..",
					"com.educational.platform.course.enrollments..",
					"com.educational.platform.course.reviews..",
					"com.educational.platform.users..")
			.because("The contracts module must stay neutral and not depend on any bounded context.");

	@ArchTest
	public static final ArchRule otherContexts_shouldNotDependOn_administrationIntegrationEvents = noClasses()
			.that()
			.resideOutsideOfPackage("com.educational.platform.administration..")
			.should()
			.dependOnClassesThat()
			.resideInAPackage("com.educational.platform.administration.integration.event..")
			.because("Context-local integration events must not be used across bounded contexts; "
					+ "cross-context events belong to the contracts module.");

}
