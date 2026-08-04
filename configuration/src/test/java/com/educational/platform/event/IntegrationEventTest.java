package com.educational.platform.event;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

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
	public static final ArchRule integrationEventHandlers_shouldDeclare_asyncEventListenerMethod = classes()
			.that()
			.haveSimpleNameEndingWith("IntegrationEventHandler")
			.should(declareAsyncEventListenerMethod())
			.because("Integration event handlers should consume events asynchronously via Spring's event mechanism.");

	private static ArchCondition<JavaClass> declareAsyncEventListenerMethod() {
		return new ArchCondition<>("declare a method annotated with @EventListener and @Async") {
			@Override
			public void check(JavaClass javaClass, ConditionEvents events) {
				final boolean satisfied = javaClass.getMethods().stream()
						.anyMatch(method -> method.isAnnotatedWith(EventListener.class) && method.isAnnotatedWith(Async.class));
				final String message = String.format("Class %s %s a method annotated with @EventListener and @Async",
						javaClass.getName(), satisfied ? "declares" : "does not declare");
				events.add(new SimpleConditionEvent(javaClass, satisfied, message));
			}
		};
	}

}
