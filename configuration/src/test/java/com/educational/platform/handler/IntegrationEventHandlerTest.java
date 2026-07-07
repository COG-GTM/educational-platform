package com.educational.platform.handler;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import org.springframework.resilience.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.educational.platform.IntegrationEventsAsyncConfiguration;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

@AnalyzeClasses(packages = "com.educational.platform")
public class IntegrationEventHandlerTest {

    @ArchTest
    public static final ArchRule integrationEventHandlerMethods_shouldBe_asyncAfterCommitAndRetryable = methods()
            .that().arePublic()
            .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("IntegrationEventHandler")
            .should(new ArchCondition<>("be annotated with @Async(\"integrationEventExecutor\"), @Retryable and @TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    if (!method.isAnnotatedWith(Async.class)) {
                        events.add(SimpleConditionEvent.violated(method, method.getFullName() + " is not annotated with @Async"));
                    } else if (!IntegrationEventsAsyncConfiguration.INTEGRATION_EVENT_EXECUTOR.equals(method.getAnnotationOfType(Async.class).value())) {
                        events.add(SimpleConditionEvent.violated(method, method.getFullName() + " does not use the '" + IntegrationEventsAsyncConfiguration.INTEGRATION_EVENT_EXECUTOR + "' executor"));
                    }
                    if (!method.isAnnotatedWith(Retryable.class)) {
                        events.add(SimpleConditionEvent.violated(method, method.getFullName() + " is not annotated with @Retryable"));
                    }
                    if (!method.isAnnotatedWith(TransactionalEventListener.class)) {
                        events.add(SimpleConditionEvent.violated(method, method.getFullName() + " is not annotated with @TransactionalEventListener"));
                    } else {
                        final TransactionalEventListener listener = method.getAnnotationOfType(TransactionalEventListener.class);
                        if (listener.phase() != TransactionPhase.AFTER_COMMIT) {
                            events.add(SimpleConditionEvent.violated(method, method.getFullName() + " does not use TransactionPhase.AFTER_COMMIT"));
                        }
                        if (!listener.fallbackExecution()) {
                            events.add(SimpleConditionEvent.violated(method, method.getFullName() + " does not enable fallbackExecution"));
                        }
                    }
                }
            })
            .because("Integration events must be processed after a successful commit, on the bounded integration event executor, with retries on transient failures (see ADR-0014).");

}
