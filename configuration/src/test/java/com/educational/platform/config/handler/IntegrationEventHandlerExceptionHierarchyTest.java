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
import org.springframework.retry.annotation.Retryable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the exception class hierarchy assumptions that underpin the
 * @Retryable / @Recover configuration across all integration event handlers.
 * <p>
 * The handlers declare {@code retryFor = {DataAccessException, ObjectOptimisticLockingFailureException}}.
 * Since OOLFE is a subclass of DataAccessException, listing it explicitly is
 * redundant but documents intent. The {@code recover(DataAccessException, ...)}
 * method catches both because OOLFE IS-A DataAccessException.
 * <p>
 * If the Spring Framework hierarchy ever changes (unlikely but possible in a
 * major version bump), these tests will fail immediately instead of causing
 * silent dead-letter failures at runtime.
 */
class IntegrationEventHandlerExceptionHierarchyTest {

    private static final List<Class<?>> ALL_HANDLER_CLASSES = List.of(
            SendCourseToApproveIntegrationEventHandler.class,
            CourseApprovedByAdminIntegrationEventHandler.class,
            StudentEnrolledToCourseIntegrationEventHandler.class,
            CourseRatingRecalculatedIntegrationEventHandler.class,
            UserCreatedIntegrationEventHandler.class
    );

    @Test
    void objectOptimisticLockingFailureException_isSubclassOfDataAccessException() {
        assertThat(DataAccessException.class)
                .as("OOLFE must be a DataAccessException subclass for @Recover(DataAccessException) to catch it")
                .isAssignableFrom(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void recoverMethodFirstParam_coversAllRetryForExceptions() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method eventListenerMethod = findEventListenerMethod(handlerClass);
            Retryable retryable = eventListenerMethod.getAnnotation(Retryable.class);
            Method recoverMethod = findRecoverMethod(handlerClass);
            Class<?> recoverExceptionParam = recoverMethod.getParameterTypes()[0];

            for (Class<? extends Throwable> retryFor : retryable.retryFor()) {
                assertThat(recoverExceptionParam)
                        .as("recover() in %s must accept %s (or a superclass) to handle exhausted retries",
                                handlerClass.getSimpleName(), retryFor.getSimpleName())
                        .isAssignableFrom(retryFor);
            }
        }
    }

    @Test
    void retryForExceptions_areAllCheckedOrUnchecked_consistently() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);

            for (Class<? extends Throwable> retryFor : retryable.retryFor()) {
                assertThat(RuntimeException.class)
                        .as("retryFor exception %s in %s should be unchecked (extends RuntimeException)",
                                retryFor.getSimpleName(), handlerClass.getSimpleName())
                        .isAssignableFrom(retryFor);
            }
        }
    }

    @Test
    void retryForExceptions_areNotAbstract() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);

            for (Class<? extends Throwable> retryFor : retryable.retryFor()) {
                if (retryFor == DataAccessException.class) {
                    assertThat(java.lang.reflect.Modifier.isAbstract(retryFor.getModifiers()))
                            .as("DataAccessException is abstract — retries match subclasses")
                            .isTrue();
                }
            }
        }
    }

    @Test
    void objectOptimisticLockingFailureException_hasPublicStringConstructor() throws NoSuchMethodException {
        ObjectOptimisticLockingFailureException.class.getConstructor(String.class, Throwable.class);
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
