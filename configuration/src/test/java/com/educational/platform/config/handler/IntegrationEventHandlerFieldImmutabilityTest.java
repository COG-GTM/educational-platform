package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.teacher.create.UserCreatedIntegrationEventHandler;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that all integration event handler instance fields are {@code private final}.
 * <p>
 * Handlers are Spring singleton beans invoked concurrently from the async thread pool.
 * Non-final instance fields would create race conditions; non-private fields would
 * break encapsulation and allow external mutation of handler state.
 * <p>
 * Static fields (log, MAX_ATTEMPTS) are excluded since they are shared constants.
 */
class IntegrationEventHandlerFieldImmutabilityTest {

    private static final List<Class<?>> ALL_HANDLER_CLASSES = List.of(
            SendCourseToApproveIntegrationEventHandler.class,
            CourseApprovedByAdminIntegrationEventHandler.class,
            StudentEnrolledToCourseIntegrationEventHandler.class,
            CourseRatingRecalculatedIntegrationEventHandler.class,
            UserCreatedIntegrationEventHandler.class
    );

    @Test
    void allHandlers_instanceFields_arePrivate() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Field[] instanceFields = getInstanceFields(handlerClass);
            for (Field field : instanceFields) {
                assertThat(Modifier.isPrivate(field.getModifiers()))
                        .as("Instance field '%s' in %s must be private for encapsulation",
                                field.getName(), handlerClass.getSimpleName())
                        .isTrue();
            }
        }
    }

    @Test
    void allHandlers_instanceFields_areFinal() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Field[] instanceFields = getInstanceFields(handlerClass);
            for (Field field : instanceFields) {
                assertThat(Modifier.isFinal(field.getModifiers()))
                        .as("Instance field '%s' in %s must be final — singleton beans are accessed "
                                + "concurrently from the async thread pool", field.getName(), handlerClass.getSimpleName())
                        .isTrue();
            }
        }
    }

    @Test
    void allHandlers_haveExactlyTwoInstanceFields() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Field[] instanceFields = getInstanceFields(handlerClass);
            assertThat(instanceFields)
                    .as("Handler %s should have exactly 2 instance fields (commandHandler + repository)",
                            handlerClass.getSimpleName())
                    .hasSize(2);
        }
    }

    @Test
    void allHandlers_staticFields_areFinal() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Field[] staticFields = getStaticFields(handlerClass);
            for (Field field : staticFields) {
                assertThat(Modifier.isFinal(field.getModifiers()))
                        .as("Static field '%s' in %s must be final — mutable static state is never thread-safe",
                                field.getName(), handlerClass.getSimpleName())
                        .isTrue();
            }
        }
    }

    @Test
    void allHandlers_staticFields_arePrivate() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Field[] staticFields = getStaticFields(handlerClass);
            for (Field field : staticFields) {
                assertThat(Modifier.isPrivate(field.getModifiers()))
                        .as("Static field '%s' in %s must be private",
                                field.getName(), handlerClass.getSimpleName())
                        .isTrue();
            }
        }
    }

    @Test
    void allHandlers_haveExactlyTwoStaticFields() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Field[] staticFields = getStaticFields(handlerClass);
            assertThat(staticFields)
                    .as("Handler %s should have exactly 2 static fields (log + MAX_ATTEMPTS)",
                            handlerClass.getSimpleName())
                    .hasSize(2);
        }
    }

    @Test
    void allHandlers_noVolatileFields() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            for (Field field : handlerClass.getDeclaredFields()) {
                assertThat(Modifier.isVolatile(field.getModifiers()))
                        .as("Field '%s' in %s should NOT be volatile — all fields are final, "
                                + "volatile indicates mutable shared state", field.getName(), handlerClass.getSimpleName())
                        .isFalse();
            }
        }
    }

    @Test
    void allHandlers_noTransientFields() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            for (Field field : handlerClass.getDeclaredFields()) {
                assertThat(Modifier.isTransient(field.getModifiers()))
                        .as("Field '%s' in %s should NOT be transient — handlers are not serializable",
                                field.getName(), handlerClass.getSimpleName())
                        .isFalse();
            }
        }
    }

    private Field[] getInstanceFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .toArray(Field[]::new);
    }

    private Field[] getStaticFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .toArray(Field[]::new);
    }
}
