package com.educational.platform.config;

import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.teacher.create.UserCreatedIntegrationEventHandler;
import com.educational.platform.common.event.IntegrationEventRetryHandler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.event.EventListener;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies all 5 integration event handlers maintain identical retry/recover contract.
 * Catches drift where one handler's configuration diverges from the others.
 */
class IntegrationEventHandlerContractTest {

    static Stream<Class<?>> handlerClasses() {
        return Stream.of(
                SendCourseToApproveIntegrationEventHandler.class,
                CourseApprovedByAdminIntegrationEventHandler.class,
                StudentEnrolledToCourseIntegrationEventHandler.class,
                CourseRatingRecalculatedIntegrationEventHandler.class,
                UserCreatedIntegrationEventHandler.class
        );
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_haveComponentAnnotation(Class<?> handlerClass) {
        assertThat(handlerClass.isAnnotationPresent(Component.class))
                .as("%s should have @Component", handlerClass.getSimpleName())
                .isTrue();
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_haveExactlyOneEventListenerMethod(Class<?> handlerClass) {
        long eventListenerMethods = Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(EventListener.class))
                .count();

        assertThat(eventListenerMethods)
                .as("%s should have exactly 1 @EventListener method", handlerClass.getSimpleName())
                .isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_eventListenerMethodHasAsyncAnnotation(Class<?> handlerClass) {
        Method handlerMethod = findEventListenerMethod(handlerClass);

        assertThat(handlerMethod.isAnnotationPresent(Async.class))
                .as("%s handler method should have @Async", handlerClass.getSimpleName())
                .isTrue();
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_retryableConfigIsConsistent(Class<?> handlerClass) {
        Method handlerMethod = findEventListenerMethod(handlerClass);
        Retryable retryable = handlerMethod.getAnnotation(Retryable.class);

        assertThat(retryable)
                .as("%s handler method should have @Retryable", handlerClass.getSimpleName())
                .isNotNull();
        assertThat(retryable.maxAttempts())
                .as("%s maxAttempts", handlerClass.getSimpleName())
                .isEqualTo(3);
        assertThat(retryable.backoff().delay())
                .as("%s backoff delay", handlerClass.getSimpleName())
                .isEqualTo(500);
        assertThat(retryable.backoff().multiplier())
                .as("%s backoff multiplier", handlerClass.getSimpleName())
                .isEqualTo(2);
        assertThat(retryable.noRetryFor())
                .as("%s should have no exclusions", handlerClass.getSimpleName())
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_retryForMatchesCentralizedRetryableExceptions(Class<?> handlerClass) {
        Method handlerMethod = findEventListenerMethod(handlerClass);
        Retryable retryable = handlerMethod.getAnnotation(Retryable.class);

        assertThat(retryable.retryFor())
                .as("%s retryFor types should match IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS",
                        handlerClass.getSimpleName())
                .containsExactlyInAnyOrder(
                        (Class[]) IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS
                );
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_haveExactlyOneRecoverMethod(Class<?> handlerClass) {
        long recoverMethods = Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Recover.class))
                .count();

        assertThat(recoverMethods)
                .as("%s should have exactly 1 @Recover method", handlerClass.getSimpleName())
                .isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_recoverMethodAcceptsTransientDataAccessException(Class<?> handlerClass) {
        Method recoverMethod = findRecoverMethod(handlerClass);

        assertThat(recoverMethod.getParameterTypes()[0])
                .as("%s @Recover first param should be TransientDataAccessException",
                        handlerClass.getSimpleName())
                .isEqualTo(TransientDataAccessException.class);
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_recoverMethodReturnsVoid(Class<?> handlerClass) {
        Method recoverMethod = findRecoverMethod(handlerClass);

        assertThat(recoverMethod.getReturnType())
                .as("%s @Recover should return void", handlerClass.getSimpleName())
                .isEqualTo(void.class);
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_eventListenerMethodIsPublic(Class<?> handlerClass) {
        Method handlerMethod = findEventListenerMethod(handlerClass);

        assertThat(Modifier.isPublic(handlerMethod.getModifiers()))
                .as("%s handler method should be public for AOP proxying",
                        handlerClass.getSimpleName())
                .isTrue();
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_recoverMethodIsPublic(Class<?> handlerClass) {
        Method recoverMethod = findRecoverMethod(handlerClass);

        assertThat(Modifier.isPublic(recoverMethod.getModifiers()))
                .as("%s @Recover method should be public for Spring Retry",
                        handlerClass.getSimpleName())
                .isTrue();
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_eventListenerMethodReturnsVoid(Class<?> handlerClass) {
        Method handlerMethod = findEventListenerMethod(handlerClass);

        assertThat(handlerMethod.getReturnType())
                .as("%s handler method should return void", handlerClass.getSimpleName())
                .isEqualTo(void.class);
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_handlerAndRecoverReturnTypesMatch(Class<?> handlerClass) {
        Method handlerMethod = findEventListenerMethod(handlerClass);
        Method recoverMethod = findRecoverMethod(handlerClass);

        assertThat(handlerMethod.getReturnType())
                .as("%s handler and recover return types must match for Spring Retry",
                        handlerClass.getSimpleName())
                .isEqualTo(recoverMethod.getReturnType());
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_recoverParameterTypeCoversAllRetryForTypes(Class<?> handlerClass) {
        Method handlerMethod = findEventListenerMethod(handlerClass);
        Retryable retryable = handlerMethod.getAnnotation(Retryable.class);
        Method recoverMethod = findRecoverMethod(handlerClass);
        Class<?> recoverExceptionType = recoverMethod.getParameterTypes()[0];

        for (Class<? extends Throwable> retryForType : retryable.retryFor()) {
            assertThat(recoverExceptionType.isAssignableFrom(retryForType))
                    .as("%s: @Recover param %s should be assignable from retryFor type %s",
                            handlerClass.getSimpleName(),
                            recoverExceptionType.getSimpleName(),
                            retryForType.getSimpleName())
                    .isTrue();
        }
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_noRecoverMethodAcceptingGenericException(Class<?> handlerClass) {
        boolean hasGenericRecover = Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Recover.class))
                .anyMatch(m -> m.getParameterTypes()[0] == Exception.class
                        || m.getParameterTypes()[0] == RuntimeException.class);

        assertThat(hasGenericRecover)
                .as("%s should not have @Recover accepting Exception/RuntimeException",
                        handlerClass.getSimpleName())
                .isFalse();
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_recoverMethodHasTwoParameters(Class<?> handlerClass) {
        Method recoverMethod = findRecoverMethod(handlerClass);

        assertThat(recoverMethod.getParameterCount())
                .as("%s @Recover method should have 2 params (exception, event)",
                        handlerClass.getSimpleName())
                .isEqualTo(2);
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_eventListenerMethodHasOneParameter(Class<?> handlerClass) {
        Method handlerMethod = findEventListenerMethod(handlerClass);

        assertThat(handlerMethod.getParameterCount())
                .as("%s handler method should have 1 param (the event)",
                        handlerClass.getSimpleName())
                .isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_recoverSecondParamMatchesEventListenerParam(Class<?> handlerClass) {
        Method handlerMethod = findEventListenerMethod(handlerClass);
        Method recoverMethod = findRecoverMethod(handlerClass);

        Class<?> eventListenerParamType = handlerMethod.getParameterTypes()[0];
        Class<?> recoverSecondParamType = recoverMethod.getParameterTypes()[1];

        assertThat(recoverSecondParamType)
                .as("%s @Recover second param must match @EventListener param for Spring Retry binding",
                        handlerClass.getSimpleName())
                .isEqualTo(eventListenerParamType);
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_haveFailedIntegrationEventRepositoryDependency(Class<?> handlerClass) {
        boolean hasRepoDependency = Arrays.stream(handlerClass.getDeclaredConstructors())
                .anyMatch(c -> Arrays.stream(c.getParameterTypes())
                        .anyMatch(p -> p == com.educational.platform.common.event.FailedIntegrationEventRepository.class));

        assertThat(hasRepoDependency)
                .as("%s should depend on FailedIntegrationEventRepository",
                        handlerClass.getSimpleName())
                .isTrue();
    }

    @ParameterizedTest
    @MethodSource("handlerClasses")
    void allHandlers_recoverMethodDoesNotHaveEventListenerAnnotation(Class<?> handlerClass) {
        Method recoverMethod = findRecoverMethod(handlerClass);

        assertThat(recoverMethod.isAnnotationPresent(EventListener.class))
                .as("%s @Recover should not also be @EventListener", handlerClass.getSimpleName())
                .isFalse();
        assertThat(recoverMethod.isAnnotationPresent(Async.class))
                .as("%s @Recover should not also be @Async", handlerClass.getSimpleName())
                .isFalse();
    }

    @Test
    void allHandlers_totalCountIsFive() {
        assertThat(handlerClasses().count())
                .as("Should test exactly 5 integration event handlers")
                .isEqualTo(5);
    }

    private Method findEventListenerMethod(Class<?> handlerClass) {
        return Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(EventListener.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        handlerClass.getSimpleName() + " has no @EventListener method"));
    }

    private Method findRecoverMethod(Class<?> handlerClass) {
        return Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Recover.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        handlerClass.getSimpleName() + " has no @Recover method"));
    }
}
