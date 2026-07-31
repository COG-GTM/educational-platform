package com.educational.platform.event;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.educational.platform")
public class IntegrationEventContractTest {

	@ArchTest
	public static final ArchRule contract_events_shouldBe_records = classes()
			.that()
			.resideInAPackage("com.educational.platform.integration.event")
			.should()
			.beRecords()
			.because("Shared contract events should be immutable records.");

	@ArchTest
	public static final ArchRule contract_shouldNotDependOn_boundedContexts = noClasses()
			.that()
			.resideInAPackage("com.educational.platform.integration.event..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage(
					"com.educational.platform.courses..",
					"com.educational.platform.administration..",
					"com.educational.platform.users..",
					"com.educational.platform.course.enrollments..",
					"com.educational.platform.course.reviews..")
			.because("The shared contract module must be owned by no bounded context.");

	@ArchTest
	public static final ArchRule administration_shouldNotDependOn_coursesIntegrationEvents = noClasses()
			.that()
			.resideInAPackage("com.educational.platform.administration..")
			.should()
			.dependOnClassesThat()
			.resideInAPackage("com.educational.platform.courses.integration.event..")
			.because("Administration must consume shared contract events, not courses-owned events.");

}
