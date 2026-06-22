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

import com.educational.platform.common.event.FailedIntegrationEventRepository;

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

    private Method findEventListenerMethod(Class<?> handlerClass) {
        return Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> m.getAnnotation(EventListener.class) != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No @EventListener method found in " + handlerClass.getSimpleName()));
    }

    private Method findRecoverMethod(Class<?> handlerClass) {
        return Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> m.getAnnotation(Recover.class) != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No @Recover method found in " + handlerClass.getSimpleName()));
    }
}
