package com.educational.platform.event;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.educational.platform")
public class IntegrationEventTest {

	@ArchTest
	public static final ArchRule events_shouldBe_immutable = fields()
			.that()
			.areDeclaredInClassesThat()
			.haveSimpleNameEndingWith("Event")
			.should()
			.beFinal()
			.because("Events should be immutable.");

	@ArchTest
	public static final ArchRule integrationEvents_shouldReside_inIntegrationEventPackage = classes()
			.that()
			.haveSimpleNameEndingWith("IntegrationEvent")
			.should()
			.resideInAPackage("..integration.event..")
			.because("Integration events form the public contract between modules and should live in the integration.event package.");

	@ArchTest
	public static final ArchRule eventListenerMethods_shouldBeDeclared_inIntegrationEventHandlers = methods()
			.that()
			.areMetaAnnotatedWith(EventListener.class)
			.should()
			.beDeclaredInClassesThat()
			.haveSimpleNameEndingWith("IntegrationEventHandler")
			.because("Integration event listeners should be encapsulated in dedicated *IntegrationEventHandler classes.");

	@ArchTest
	public static final ArchRule eventListenerMethods_shouldBe_asynchronous = methods()
			.that()
			.areAnnotatedWith(EventListener.class)
			.should()
			.beAnnotatedWith(Async.class)
			.because("Integration events cross module boundaries and must be handled asynchronously to keep modules decoupled.");

}
