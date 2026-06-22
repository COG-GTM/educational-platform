package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.teacher.create.UserCreatedIntegrationEventHandler;

import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Recover;
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Additional contract tests for integration event handlers, covering:
 * 1. @Async value is empty (uses default AsyncConfig executor) across all handlers
 * 2. Handler method naming follows handle*Event convention
 * 3. ObjectOptimisticLockingFailureException is a DataAccessException subclass (critical for @Recover matching)
 * 4. @Recover method name is consistently "recover" across all handlers
 * 5. Handler classes do not implement any interfaces (Spring proxies via CGLIB)
 */
class IntegrationEventHandlerContractTest {

    private static final List<Class<?>> ALL_HANDLER_CLASSES = List.of(
            SendCourseToApproveIntegrationEventHandler.class,
            CourseApprovedByAdminIntegrationEventHandler.class,
            StudentEnrolledToCourseIntegrationEventHandler.class,
            CourseRatingRecalculatedIntegrationEventHandler.class,
            UserCreatedIntegrationEventHandler.class
    );

    @Test
    void allHandlers_asyncAnnotation_usesDefaultExecutor() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Async async = method.getAnnotation(Async.class);
            assertThat(async.value())
                    .as("@Async.value in %s should be empty (uses AsyncConfig's default executor)",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_eventListenerMethodName_startsWithHandle() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            assertThat(method.getName())
                    .as("@EventListener method in %s should start with 'handle'", handlerClass.getSimpleName())
                    .startsWith("handle");
        }
    }

    @Test
    void allHandlers_eventListenerMethodName_endsWithEvent() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            assertThat(method.getName())
                    .as("@EventListener method in %s should end with 'Event'", handlerClass.getSimpleName())
                    .endsWith("Event");
        }
    }

    @Test
    void allHandlers_recoverMethodName_isRecover() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findRecoverMethod(handlerClass);
            assertThat(method.getName())
                    .as("@Recover method in %s should be named 'recover'", handlerClass.getSimpleName())
                    .isEqualTo("recover");
        }
    }

    @Test
    void allHandlers_doNotImplementAnyInterfaces() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getInterfaces())
                    .as("Handler %s should not implement interfaces (CGLIB proxies required for @Async + @Retryable)",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void allHandlers_extendObject() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getSuperclass())
                    .as("Handler %s should directly extend Object", handlerClass.getSimpleName())
                    .isEqualTo(Object.class);
        }
    }

    @Test
    void objectOptimisticLockingFailureException_isDataAccessExceptionSubclass() {
        // Critical contract: the single @Recover(DataAccessException) method handles BOTH
        // retryFor exception types because ObjectOptimisticLockingFailureException is a
        // DataAccessException subclass. If this hierarchy changes (e.g., Spring upgrade),
        // the @Recover method will silently stop catching optimistic lock failures.
        assertThat(DataAccessException.class)
                .isAssignableFrom(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void allHandlers_recoverFirstParam_isAssignableFromAllRetryForExceptions() {
        // Verifies that the @Recover parameter type can catch ALL configured retryFor exceptions
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method recoverMethod = findRecoverMethod(handlerClass);
            Class<?> recoverExceptionType = recoverMethod.getParameterTypes()[0];
            assertThat(recoverExceptionType)
                    .as("@Recover exception type in %s must be assignable from DataAccessException",
                            handlerClass.getSimpleName())
                    .isAssignableFrom(org.springframework.dao.DataAccessException.class);
            assertThat(recoverExceptionType)
                    .as("@Recover exception type in %s must be assignable from ObjectOptimisticLockingFailureException",
                            handlerClass.getSimpleName())
                    .isAssignableFrom(ObjectOptimisticLockingFailureException.class);
        }
    }

    @Test
    void allHandlers_eventParameterType_isRecord() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Class<?> eventType = method.getParameterTypes()[0];
            assertThat(eventType.isRecord())
                    .as("Event type %s used by %s should be a Java record",
                            eventType.getSimpleName(), handlerClass.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void allHandlers_eventParameterType_nameEndsWithIntegrationEvent() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Class<?> eventType = method.getParameterTypes()[0];
            assertThat(eventType.getSimpleName())
                    .as("Event type in %s should end with 'IntegrationEvent'", handlerClass.getSimpleName())
                    .endsWith("IntegrationEvent");
        }
    }

    @Test
    void allHandlers_className_endsWithIntegrationEventHandler() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getSimpleName())
                    .as("Handler class name should end with 'IntegrationEventHandler'")
                    .endsWith("IntegrationEventHandler");
        }
    }

    @Test
    void allHandlers_areInDifferentPackages() {
        long distinctPackages = ALL_HANDLER_CLASSES.stream()
                .map(c -> c.getPackage().getName())
                .distinct()
                .count();
        assertThat(distinctPackages)
                .as("Each handler should reside in its own package (bounded context separation)")
                .isEqualTo(ALL_HANDLER_CLASSES.size());
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
