package com.educational.platform.event;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;

@AnalyzeClasses(packages = "com.educational.platform")
public class IntegrationEventTest {

	@ArchTest
	public static final ArchRule events_shouldBe_immutable = fields()
			.that()
			.areDeclaredInClassesThat()
			.haveSimpleNameEndingWith("Event")
			.and()
			.areDeclaredInClassesThat()
			.areNotAnnotatedWith(Entity.class)
			.should()
			.beFinal()
			.because("Events should be immutable. JPA @Entity classes are excluded as they are mutable persistence records managed by the provider.");

}
