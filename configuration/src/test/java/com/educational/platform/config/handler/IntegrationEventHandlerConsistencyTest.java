package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.teacher.create.UserCreatedIntegrationEventHandler;

import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.educational.platform.common.event.FailedIntegrationEventRepository;

import org.springframework.dao.DataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-handler consistency test: all 5 integration event handlers must follow the
 * same retry/async/recover pattern. Guards against accidental config drift.
 */
class IntegrationEventHandlerConsistencyTest {

    private static final List<Class<?>> ALL_HANDLER_CLASSES = List.of(
            SendCourseToApproveIntegrationEventHandler.class,
            CourseApprovedByAdminIntegrationEventHandler.class,
            StudentEnrolledToCourseIntegrationEventHandler.class,
            CourseRatingRecalculatedIntegrationEventHandler.class,
            UserCreatedIntegrationEventHandler.class
    );

    @Test
    void allHandlers_haveComponentAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getAnnotation(Component.class))
                    .as("Handler %s should have @Component", handlerClass.getSimpleName())
                    .isNotNull();
        }
    }

    @Test
    void allHandlers_haveExactlyOneEventListenerMethod() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            long count = Arrays.stream(handlerClass.getDeclaredMethods())
                    .filter(m -> m.getAnnotation(EventListener.class) != null)
                    .count();
            assertThat(count)
                    .as("Handler %s should have exactly 1 @EventListener method", handlerClass.getSimpleName())
                    .isEqualTo(1);
        }
    }

    @Test
    void allHandlers_haveExactlyOneRecoverMethod() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            long count = Arrays.stream(handlerClass.getDeclaredMethods())
                    .filter(m -> m.getAnnotation(Recover.class) != null)
                    .count();
            assertThat(count)
                    .as("Handler %s should have exactly 1 @Recover method", handlerClass.getSimpleName())
                    .isEqualTo(1);
        }
    }

    @Test
    void allHandlers_eventListenerMethod_hasAsyncAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method eventListenerMethod = findEventListenerMethod(handlerClass);
            assertThat(eventListenerMethod.getAnnotation(Async.class))
                    .as("@EventListener in %s should also have @Async", handlerClass.getSimpleName())
                    .isNotNull();
        }
    }

    @Test
    void allHandlers_retryableAnnotation_sameRetryForExceptions() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            assertThat(retryable)
                    .as("@EventListener in %s should have @Retryable", handlerClass.getSimpleName())
                    .isNotNull();
            assertThat(retryable.retryFor())
                    .as("@Retryable.retryFor in %s", handlerClass.getSimpleName())
                    .extracting(Class::getName)
                    .containsExactlyInAnyOrder(
                            "org.springframework.dao.DataAccessException",
                            "org.springframework.orm.ObjectOptimisticLockingFailureException"
                    );
        }
    }

    @Test
    void allHandlers_retryableAnnotation_sameMaxAttempts() {
        Integer expectedMaxAttempts = null;
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            if (expectedMaxAttempts == null) {
                expectedMaxAttempts = retryable.maxAttempts();
            }
            assertThat(retryable.maxAttempts())
                    .as("@Retryable.maxAttempts in %s should match other handlers", handlerClass.getSimpleName())
                    .isEqualTo(expectedMaxAttempts);
        }
        assertThat(expectedMaxAttempts).isEqualTo(3);
    }

    @Test
    void allHandlers_retryableAnnotation_sameBackoffConfig() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Backoff backoff = method.getAnnotation(Retryable.class).backoff();
            assertThat(backoff.delay())
                    .as("@Backoff.delay in %s", handlerClass.getSimpleName())
                    .isEqualTo(500);
            assertThat(backoff.multiplier())
                    .as("@Backoff.multiplier in %s", handlerClass.getSimpleName())
                    .isEqualTo(2.0);
        }
    }

    @Test
    void allHandlers_recoverMethod_firstParamIsDataAccessException() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method recoverMethod = findRecoverMethod(handlerClass);
            Class<?>[] paramTypes = recoverMethod.getParameterTypes();
            assertThat(paramTypes[0].getName())
                    .as("@Recover first param in %s should be DataAccessException", handlerClass.getSimpleName())
                    .isEqualTo("org.springframework.dao.DataAccessException");
        }
    }

    @Test
    void allHandlers_recoverMethod_doesNotAcceptBroadException() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            boolean hasBroadRecover = Arrays.stream(handlerClass.getDeclaredMethods())
                    .filter(m -> m.getAnnotation(Recover.class) != null)
                    .anyMatch(m -> m.getParameterTypes().length > 0
                            && m.getParameterTypes()[0] == Exception.class);
            assertThat(hasBroadRecover)
                    .as("Handler %s should NOT have @Recover accepting Exception.class", handlerClass.getSimpleName())
                    .isFalse();
        }
    }

    @Test
    void allHandlers_retryableAnnotation_noRetryForIsEmpty() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            assertThat(retryable.noRetryFor())
                    .as("@Retryable.noRetryFor in %s should be empty", handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_recoverMethod_returnsVoid() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method recoverMethod = findRecoverMethod(handlerClass);
            assertThat(recoverMethod.getReturnType())
                    .as("@Recover in %s should return void", handlerClass.getSimpleName())
                    .isEqualTo(void.class);
        }
    }

    @Test
    void allHandlers_haveMaxAttemptsStaticFinalField() throws Exception {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            var field = handlerClass.getDeclaredField("MAX_ATTEMPTS");
            assertThat(java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                    .as("MAX_ATTEMPTS in %s should be static", handlerClass.getSimpleName())
                    .isTrue();
            assertThat(java.lang.reflect.Modifier.isFinal(field.getModifiers()))
                    .as("MAX_ATTEMPTS in %s should be final", handlerClass.getSimpleName())
                    .isTrue();
            field.setAccessible(true);
            int maxAttempts = (int) field.get(null);
            assertThat(maxAttempts)
                    .as("MAX_ATTEMPTS value in %s", handlerClass.getSimpleName())
                    .isEqualTo(3);
        }
    }

    @Test
    void allHandlers_areNotFinal() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(Modifier.isFinal(handlerClass.getModifiers()))
                    .as("Handler %s must not be final — Spring creates CGLIB proxies for @Async and @Retryable",
                            handlerClass.getSimpleName())
                    .isFalse();
        }
    }

    @Test
    void allHandlers_recoverEventParameterMatchesHandlerEventParameter() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method handler = findEventListenerMethod(handlerClass);
            Method recover = findRecoverMethod(handlerClass);
            Class<?> handlerEventType = handler.getParameterTypes()[0];
            Class<?> recoverEventType = recover.getParameterTypes()[1];
            assertThat(recoverEventType)
                    .as("@Recover second param in %s must match @EventListener event type for convention-based discovery",
                            handlerClass.getSimpleName())
                    .isEqualTo(handlerEventType);
        }
    }

    @Test
    void allHandlers_constructorSecondParameterIsFailedIntegrationEventRepository() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getConstructors()).hasSize(1);
            Class<?>[] paramTypes = handlerClass.getConstructors()[0].getParameterTypes();
            assertThat(paramTypes[paramTypes.length - 1])
                    .as("Last constructor param in %s should be FailedIntegrationEventRepository",
                            handlerClass.getSimpleName())
                    .isEqualTo(FailedIntegrationEventRepository.class);
        }
    }

    @Test
    void allHandlers_logFieldIsPrivateStaticFinal() throws Exception {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Field logField = handlerClass.getDeclaredField("log");
            assertThat(Modifier.isPrivate(logField.getModifiers()))
                    .as("log in %s should be private", handlerClass.getSimpleName())
                    .isTrue();
            assertThat(Modifier.isStatic(logField.getModifiers()))
                    .as("log in %s should be static", handlerClass.getSimpleName())
                    .isTrue();
            assertThat(Modifier.isFinal(logField.getModifiers()))
                    .as("log in %s should be final", handlerClass.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void allHandlers_eventListenerMethod_hasRetryableAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method eventListenerMethod = findEventListenerMethod(handlerClass);
            assertThat(eventListenerMethod.getAnnotation(Retryable.class))
                    .as("@EventListener in %s should also have @Retryable", handlerClass.getSimpleName())
                    .isNotNull();
        }
    }

    @Test
    void allHandlers_recoverMethod_doesNotHaveRetryableAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method recoverMethod = findRecoverMethod(handlerClass);
            assertThat(recoverMethod.getAnnotation(Retryable.class))
                    .as("@Recover in %s should NOT have @Retryable (prevents infinite retry loops)",
                            handlerClass.getSimpleName())
                    .isNull();
        }
    }

    @Test
    void allHandlers_recoverMethod_doesNotHaveAsyncAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method recoverMethod = findRecoverMethod(handlerClass);
            assertThat(recoverMethod.getAnnotation(Async.class))
                    .as("@Recover in %s should NOT have @Async (recover runs in same thread as retry)",
                            handlerClass.getSimpleName())
                    .isNull();
        }
    }

    @Test
    void allHandlers_recoverMethod_doesNotHaveEventListenerAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method recoverMethod = findRecoverMethod(handlerClass);
            assertThat(recoverMethod.getAnnotation(EventListener.class))
                    .as("@Recover in %s should NOT have @EventListener", handlerClass.getSimpleName())
                    .isNull();
        }
    }

    @Test
    void allHandlers_retryableAnnotation_recoverAttributeIsDefault() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            assertThat(retryable.recover())
                    .as("@Retryable.recover in %s should be empty (convention-based discovery)",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_haveExactlyOnePublicConstructor() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Constructor<?>[] constructors = handlerClass.getConstructors();
            assertThat(constructors)
                    .as("Handler %s should have exactly 1 public constructor", handlerClass.getSimpleName())
                    .hasSize(1);
            assertThat(constructors[0].getParameterCount())
                    .as("Constructor in %s should take exactly 2 params (command handler + repository)",
                            handlerClass.getSimpleName())
                    .isEqualTo(2);
        }
    }

    @Test
    void allHandlers_handlerAndRecoverReturnTypesMatch() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method handlerMethod = findEventListenerMethod(handlerClass);
            Method recoverMethod = findRecoverMethod(handlerClass);
            assertThat(handlerMethod.getReturnType())
                    .as("Handler and recover return types in %s must match (Spring Retry requirement)",
                            handlerClass.getSimpleName())
                    .isEqualTo(recoverMethod.getReturnType());
        }
    }

    @Test
    void allHandlers_eventListenerMethodReturnsVoid() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method handlerMethod = findEventListenerMethod(handlerClass);
            assertThat(handlerMethod.getReturnType())
                    .as("@EventListener in %s should return void for @Async compatibility",
                            handlerClass.getSimpleName())
                    .isEqualTo(void.class);
        }
    }

    @Test
    void allHandlers_arePublicClasses() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(Modifier.isPublic(handlerClass.getModifiers()))
                    .as("Handler %s should be public for Spring proxy creation", handlerClass.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void allHandlers_eventListenerMethodIsPublic() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            assertThat(Modifier.isPublic(method.getModifiers()))
                    .as("@EventListener in %s should be public for AOP proxying", handlerClass.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void allHandlers_recoverMethodIsPublic() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findRecoverMethod(handlerClass);
            assertThat(Modifier.isPublic(method.getModifiers()))
                    .as("@Recover in %s should be public for Spring Retry discovery", handlerClass.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void allHandlers_retryableAnnotation_backoffMaxDelayIsUnlimited() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Backoff backoff = method.getAnnotation(Retryable.class).backoff();
            assertThat(backoff.maxDelay())
                    .as("@Backoff.maxDelay in %s should be 0 (unlimited)", handlerClass.getSimpleName())
                    .isEqualTo(0L);
        }
    }

    @Test
    void allHandlers_retryableAnnotation_listenersIsEmpty() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            assertThat(retryable.listeners())
                    .as("@Retryable.listeners in %s should be empty", handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_recoverMethod_hasTwoParameters() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method recoverMethod = findRecoverMethod(handlerClass);
            assertThat(recoverMethod.getParameterCount())
                    .as("@Recover in %s should have exactly 2 parameters (exception + event)",
                            handlerClass.getSimpleName())
                    .isEqualTo(2);
        }
    }

    @Test
    void allHandlers_eventListenerMethod_hasExactlyOneParameter() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            assertThat(method.getParameterCount())
                    .as("@EventListener in %s should accept exactly 1 event parameter",
                            handlerClass.getSimpleName())
                    .isEqualTo(1);
        }
    }

    @Test
    void allHandlers_retryableAnnotation_statefulIsFalse() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            assertThat(retryable.stateful())
                    .as("@Retryable.stateful in %s should be false (stateless retry within single invocation)",
                            handlerClass.getSimpleName())
                    .isFalse();
        }
    }

    @Test
    void allHandlers_retryableAnnotation_backoffRandomIsFalse() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Backoff backoff = method.getAnnotation(Retryable.class).backoff();
            assertThat(backoff.random())
                    .as("@Backoff.random in %s should be false (deterministic backoff)",
                            handlerClass.getSimpleName())
                    .isFalse();
        }
    }

    @Test
    void allHandlers_retryableAnnotation_exceptionExpressionIsEmpty() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            assertThat(retryable.exceptionExpression())
                    .as("@Retryable.exceptionExpression in %s should be empty (no SpEL conditional retry)",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_retryableAnnotation_maxAttemptsExpressionIsEmpty() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            assertThat(retryable.maxAttemptsExpression())
                    .as("@Retryable.maxAttemptsExpression in %s should be empty (maxAttempts is fixed, not SpEL-resolved)",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_retryableAnnotation_labelIsEmpty() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            assertThat(retryable.label())
                    .as("@Retryable.label in %s should be empty (no custom label set)",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_retryableAnnotation_backoffDelayIsPositive() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Backoff backoff = method.getAnnotation(Retryable.class).backoff();
            assertThat(backoff.delay())
                    .as("@Backoff.delay in %s should be positive to prevent tight retry loops",
                            handlerClass.getSimpleName())
                    .isGreaterThan(0);
        }
    }

    @Test
    void allHandlers_nonStaticFieldsArePrivateAndFinal() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            for (Field field : handlerClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                assertThat(Modifier.isPrivate(field.getModifiers()))
                        .as("Non-static field '%s' in %s should be private",
                                field.getName(), handlerClass.getSimpleName())
                        .isTrue();
                assertThat(Modifier.isFinal(field.getModifiers()))
                        .as("Non-static field '%s' in %s should be final (constructor-injected)",
                                field.getName(), handlerClass.getSimpleName())
                        .isTrue();
            }
        }
    }

    @Test
    void allHandlers_haveExactlyFourDeclaredFields() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getDeclaredFields())
                    .as("Handler %s should have exactly 4 fields (log, MAX_ATTEMPTS, commandHandler, repository)",
                            handlerClass.getSimpleName())
                    .hasSize(4);
        }
    }

    @Test
    void allHandlers_loggerReferencesOwnClass() throws Exception {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Field logField = handlerClass.getDeclaredField("log");
            logField.setAccessible(true);
            org.slf4j.Logger logger = (org.slf4j.Logger) logField.get(null);
            assertThat(logger.getName())
                    .as("Logger in %s should reference its own class (catches copy-paste errors)",
                            handlerClass.getSimpleName())
                    .isEqualTo(handlerClass.getName());
        }
    }

    @Test
    void allHandlers_constructorNotAnnotatedWithAutowired() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Constructor<?>[] constructors = handlerClass.getConstructors();
            assertThat(constructors).hasSize(1);
            assertThat(constructors[0]
                    .getAnnotation(org.springframework.beans.factory.annotation.Autowired.class))
                    .as("Constructor in %s should not need @Autowired (single-constructor auto-injection)",
                            handlerClass.getSimpleName())
                    .isNull();
        }
    }

    @Test
    void allHandlers_retryableAnnotation_retryForHasExactlyTwoExceptionClasses() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            assertThat(retryable.retryFor())
                    .as("@Retryable.retryFor in %s should specify exactly 2 exception classes",
                            handlerClass.getSimpleName())
                    .hasSize(2);
        }
    }

    @Test
    void allHandlers_retryableAnnotation_allRetryForExceptionsAreCaughtByRecoverParameter() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method handler = findEventListenerMethod(handlerClass);
            Method recover = findRecoverMethod(handlerClass);
            Class<?> recoverExceptionType = recover.getParameterTypes()[0];
            for (Class<?> retryForType : handler.getAnnotation(Retryable.class).retryFor()) {
                assertThat(recoverExceptionType)
                        .as("@Recover exception param %s in %s must be assignable from retryFor type %s",
                                recoverExceptionType.getSimpleName(), handlerClass.getSimpleName(),
                                retryForType.getSimpleName())
                        .isAssignableFrom(retryForType);
            }
        }
    }

    @Test
    void allHandlers_retryableAnnotation_backoffDelayExpressionIsEmpty() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Backoff backoff = method.getAnnotation(Retryable.class).backoff();
            assertThat(backoff.delayExpression())
                    .as("@Backoff.delayExpression in %s should be empty (fixed delay, not SpEL-resolved)",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_retryableAnnotation_backoffMultiplierExpressionIsEmpty() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Backoff backoff = method.getAnnotation(Retryable.class).backoff();
            assertThat(backoff.multiplierExpression())
                    .as("@Backoff.multiplierExpression in %s should be empty (fixed multiplier, not SpEL-resolved)",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_retryableAnnotation_backoffMultiplierIsGreaterThanOne() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Backoff backoff = method.getAnnotation(Retryable.class).backoff();
            assertThat(backoff.multiplier())
                    .as("@Backoff.multiplier in %s should be > 1 for exponential backoff",
                            handlerClass.getSimpleName())
                    .isGreaterThan(1.0);
        }
    }

    private Method findEventListenerMethod(Class<?> handlerClass) {
        return Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> m.getAnnotation(EventListener.class) != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No @EventListener method found in " + handlerClass.getSimpleName()));
    }

    @Test
    void allHandlers_asyncAnnotation_valueIsDefaultEmpty() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Async async = method.getAnnotation(Async.class);
            assertThat(async.value())
                    .as("@Async.value in %s should be empty (uses default executor)", handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_maxAttemptsFieldMatchesRetryableMaxAttempts() throws Exception {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Field field = handlerClass.getDeclaredField("MAX_ATTEMPTS");
            field.setAccessible(true);
            int fieldValue = (int) field.get(null);

            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);

            assertThat(retryable.maxAttempts())
                    .as("@Retryable.maxAttempts in %s must equal MAX_ATTEMPTS field value",
                            handlerClass.getSimpleName())
                    .isEqualTo(fieldValue);
        }
    }

    @Test
    void allHandlers_doNotExtendCustomBaseClass() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getSuperclass())
                    .as("Handler %s should extend Object directly (no base class hierarchy)",
                            handlerClass.getSimpleName())
                    .isEqualTo(Object.class);
        }
    }

    @Test
    void allHandlers_recoverMethod_nameStartsWithRecover() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method recoverMethod = findRecoverMethod(handlerClass);
            assertThat(recoverMethod.getName())
                    .as("@Recover method name in %s should be 'recover'", handlerClass.getSimpleName())
                    .isEqualTo("recover");
        }
    }

    @Test
    void allHandlers_doNotImplementInterfaces() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getInterfaces())
                    .as("Handler %s should not implement interfaces (CGLIB proxy compatibility)",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_retryableAnnotation_allRetryForExceptionsExtendRuntimeException() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            for (Class<? extends Throwable> exType : retryable.retryFor()) {
                assertThat(RuntimeException.class)
                        .as("retryFor type %s in %s must extend RuntimeException for @Async void compatibility",
                                exType.getSimpleName(), handlerClass.getSimpleName())
                        .isAssignableFrom(exType);
            }
        }
    }

    @Test
    void allHandlers_failedIntegrationEventRepositoryFieldExists() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            boolean hasRepoField = Arrays.stream(handlerClass.getDeclaredFields())
                    .anyMatch(f -> f.getType().equals(FailedIntegrationEventRepository.class));
            assertThat(hasRepoField)
                    .as("Handler %s should have a FailedIntegrationEventRepository field",
                            handlerClass.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void allHandlers_logFieldTypeIsSLF4JLogger() throws Exception {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Field logField = handlerClass.getDeclaredField("log");
            assertThat(logField.getType())
                    .as("log field in %s should be org.slf4j.Logger", handlerClass.getSimpleName())
                    .isEqualTo(org.slf4j.Logger.class);
        }
    }

    @Test
    void allHandlers_eventListenerMethodNameStartsWithHandle() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            assertThat(method.getName())
                    .as("@EventListener method name in %s should start with 'handle'", handlerClass.getSimpleName())
                    .startsWith("handle");
        }
    }

    @Test
    void allHandlers_eventListenerMethodNameContainsEvent() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            assertThat(method.getName())
                    .as("@EventListener method name in %s should contain 'Event'", handlerClass.getSimpleName())
                    .contains("Event");
        }
    }

    @Test
    void allHandlers_areNotAbstract() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(Modifier.isAbstract(handlerClass.getModifiers()))
                    .as("Handler %s must not be abstract", handlerClass.getSimpleName())
                    .isFalse();
        }
    }

    @Test
    void allHandlers_doNotImplementAnyInterface() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getInterfaces())
                    .as("Handler %s should not implement any interface", handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_eventListenerAnnotation_conditionIsEmpty() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            EventListener eventListener = method.getAnnotation(EventListener.class);
            assertThat(eventListener.condition())
                    .as("@EventListener.condition in %s should be empty (unconditional event handling)",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_eventListenerAnnotation_classesIsEmpty() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            EventListener eventListener = method.getAnnotation(EventListener.class);
            assertThat(eventListener.classes())
                    .as("@EventListener.classes in %s should be empty (event type inferred from parameter)",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_eventListenerAnnotation_idIsEmpty() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            EventListener eventListener = method.getAnnotation(EventListener.class);
            assertThat(eventListener.id())
                    .as("@EventListener.id in %s should be empty (no custom listener id)",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_retryableAnnotation_interceptorIsDefault() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            assertThat(retryable.interceptor())
                    .as("@Retryable.interceptor in %s should be empty (uses default retry interceptor)",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_retryableAnnotation_backoffMaxDelayExpressionIsEmpty() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Backoff backoff = method.getAnnotation(Retryable.class).backoff();
            assertThat(backoff.maxDelayExpression())
                    .as("@Backoff.maxDelayExpression in %s should be empty (fixed max delay, not SpEL-resolved)",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_retryableAnnotation_includesObjectOptimisticLockingFailureExceptionExplicitly() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            assertThat(Arrays.asList(retryable.retryFor()))
                    .as("retryFor in %s must explicitly include ObjectOptimisticLockingFailureException for optimistic locking support",
                            handlerClass.getSimpleName())
                    .contains(ObjectOptimisticLockingFailureException.class);
        }
    }

    @Test
    void allHandlers_publicConstructor_hasExactlyTwoParameters() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Constructor<?>[] constructors = handlerClass.getConstructors();
            assertThat(constructors).as("Handler %s should have exactly one public constructor", handlerClass.getSimpleName()).hasSize(1);
            assertThat(constructors[0].getParameterCount())
                    .as("Public constructor in %s should accept exactly 2 parameters (commandHandler + repository)",
                            handlerClass.getSimpleName())
                    .isEqualTo(2);
        }
    }

    @Test
    void allHandlers_classDoesNotHaveTransactionalAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getAnnotation(Transactional.class))
                    .as("Handler %s should NOT have class-level @Transactional — @Async runs in a new thread "
                            + "where the caller's transaction context is unavailable", handlerClass.getSimpleName())
                    .isNull();
        }
    }

    @Test
    void allHandlers_handlerMethodDoesNotHaveTransactionalAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            assertThat(method.getAnnotation(Transactional.class))
                    .as("@EventListener in %s should NOT have @Transactional — the command handler manages "
                            + "its own transaction boundary", handlerClass.getSimpleName())
                    .isNull();
        }
    }

    @Test
    void allHandlers_recoverMethodDoesNotHaveTransactionalAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findRecoverMethod(handlerClass);
            assertThat(method.getAnnotation(Transactional.class))
                    .as("@Recover in %s should NOT have @Transactional — recovery persists the dead-letter "
                            + "record via its own repository.save() call", handlerClass.getSimpleName())
                    .isNull();
        }
    }

    @Test
    void allHandlers_maxAttemptsFieldValue_isConsistentAcrossAllHandlers() throws Exception {
        Integer firstValue = null;
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Field field = handlerClass.getDeclaredField("MAX_ATTEMPTS");
            field.setAccessible(true);
            int value = (int) field.get(null);
            if (firstValue == null) {
                firstValue = value;
            }
            assertThat(value)
                    .as("MAX_ATTEMPTS field value in %s should be consistent across all handlers",
                            handlerClass.getSimpleName())
                    .isEqualTo(firstValue);
        }
    }

    @Test
    void allHandlers_constructorFirstParameterIsNotFailedIntegrationEventRepository() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Class<?>[] paramTypes = handlerClass.getConstructors()[0].getParameterTypes();
            assertThat(paramTypes[0])
                    .as("First constructor param in %s should be the command handler, not the repository",
                            handlerClass.getSimpleName())
                    .isNotEqualTo(FailedIntegrationEventRepository.class);
        }
    }

    @Test
    void allHandlers_eventListenerMethod_doesNotDeclareCheckedExceptions() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            assertThat(method.getExceptionTypes())
                    .as("@EventListener in %s should not declare checked exceptions — "
                            + "@Async void methods silently swallow checked exceptions without triggering "
                            + "AsyncUncaughtExceptionHandler properly", handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_recoverMethod_doesNotDeclareCheckedExceptions() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findRecoverMethod(handlerClass);
            assertThat(method.getExceptionTypes())
                    .as("@Recover in %s should not declare checked exceptions — "
                            + "recovery runs in the same async thread and should handle failures internally",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_eventListenerMethod_isNotFinal() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            assertThat(Modifier.isFinal(method.getModifiers()))
                    .as("@EventListener method in %s must not be final — CGLIB proxies override it for @Async and @Retryable",
                            handlerClass.getSimpleName())
                    .isFalse();
        }
    }

    @Test
    void allHandlers_recoverMethod_isNotFinal() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findRecoverMethod(handlerClass);
            assertThat(Modifier.isFinal(method.getModifiers()))
                    .as("@Recover method in %s must not be final — Spring Retry discovers and invokes it via proxy",
                            handlerClass.getSimpleName())
                    .isFalse();
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    void allHandlers_retryableValueAlias_isEmpty() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            assertThat(retryable.value())
                    .as("@Retryable.value() in %s should be empty — use retryFor() instead",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    void allHandlers_retryableIncludeAlias_isEmpty() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            assertThat(retryable.include())
                    .as("@Retryable.include() in %s should be empty — use retryFor() instead",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    void allHandlers_retryableExcludeAlias_isEmpty() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            assertThat(retryable.exclude())
                    .as("@Retryable.exclude() in %s should be empty — use noRetryFor() instead",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_backoffValueAlias_isDefaultWhenDelayIsSet() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Backoff backoff = method.getAnnotation(Retryable.class).backoff();
            assertThat(backoff.delay())
                    .as("@Backoff.delay() in %s should be explicitly set (overrides value())",
                            handlerClass.getSimpleName())
                    .isGreaterThan(0);
            assertThat(backoff.value())
                    .as("@Backoff.value() in %s should be default 1000 — delay() overrides it",
                            handlerClass.getSimpleName())
                    .isEqualTo(1000L);
        }
    }

    @Test
    void allHandlers_haveExactlyTwoDeclaredPublicMethods() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            long publicMethodCount = Arrays.stream(handlerClass.getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers()))
                    .count();
            assertThat(publicMethodCount)
                    .as("Handler %s should have exactly 2 public methods (handle + recover) — "
                            + "additional public methods may be accidentally proxied by Spring AOP",
                            handlerClass.getSimpleName())
                    .isEqualTo(2);
        }
    }

    @Test
    void allHandlers_eventParameterType_toStringIncludesAllComponentNames() throws Exception {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Class<?> eventType = method.getParameterTypes()[0];
            assertThat(eventType.isRecord()).isTrue();

            var components = eventType.getRecordComponents();
            assertThat(components)
                    .as("Event type %s in %s should have at least one record component for diagnostics",
                            eventType.getSimpleName(), handlerClass.getSimpleName())
                    .isNotEmpty();

            // Instantiate with default values to verify toString() format
            Object[] args = Arrays.stream(components)
                    .map(c -> defaultValueFor(c.getType()))
                    .toArray();
            Object event = eventType.getDeclaredConstructors()[0].newInstance(args);
            String toStringOutput = event.toString();

            // Verify toString() includes all component names (critical for dead-letter diagnostics)
            for (var component : components) {
                assertThat(toStringOutput)
                        .as("Event %s toString() should include component name '%s' for dead-letter record diagnostics",
                                eventType.getSimpleName(), component.getName())
                        .contains(component.getName());
            }
        }
    }

    @Test
    void allHandlers_eventListenerMethodIsNotStatic() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            assertThat(Modifier.isStatic(method.getModifiers()))
                    .as("@EventListener in %s must not be static — CGLIB cannot proxy static methods",
                            handlerClass.getSimpleName())
                    .isFalse();
        }
    }

    @Test
    void allHandlers_recoverMethodIsNotStatic() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findRecoverMethod(handlerClass);
            assertThat(Modifier.isStatic(method.getModifiers()))
                    .as("@Recover in %s must not be static — Spring Retry discovers and invokes it via proxy",
                            handlerClass.getSimpleName())
                    .isFalse();
        }
    }

    @Test
    void allHandlers_doNotHaveServiceAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getAnnotation(org.springframework.stereotype.Service.class))
                    .as("Handler %s should use @Component, not @Service — handlers are infrastructure, not business services",
                            handlerClass.getSimpleName())
                    .isNull();
        }
    }

    @Test
    void allHandlers_doNotHaveRepositoryAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getAnnotation(org.springframework.stereotype.Repository.class))
                    .as("Handler %s should use @Component, not @Repository — handlers are not data access objects",
                            handlerClass.getSimpleName())
                    .isNull();
        }
    }

    private Object defaultValueFor(Class<?> type) {
        if (type == java.util.UUID.class) return java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");
        if (type == String.class) return "test-value";
        if (type == double.class || type == Double.class) return 0.0;
        if (type == int.class || type == Integer.class) return 0;
        if (type == long.class || type == Long.class) return 0L;
        if (type == boolean.class || type == Boolean.class) return false;
        throw new IllegalArgumentException("Unsupported type for default value: " + type);
    }

    private Method findRecoverMethod(Class<?> handlerClass) {
        return Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> m.getAnnotation(Recover.class) != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No @Recover method found in " + handlerClass.getSimpleName()));
    }
}
