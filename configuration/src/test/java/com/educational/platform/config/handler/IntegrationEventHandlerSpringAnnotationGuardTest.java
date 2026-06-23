package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.teacher.create.UserCreatedIntegrationEventHandler;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against accidental addition of Spring annotations that would alter handler
 * registration or lifecycle behavior in ways incompatible with the retry/async pattern.
 * <p>
 * Handlers must be singleton-scoped, eagerly initialized beans with no ordering or
 * conditional constraints. Adding any of these annotations would subtly break the
 * integration event processing:
 * <ul>
 *   <li>{@code @Scope("prototype")} — each event dispatch creates a new instance, losing retry state</li>
 *   <li>{@code @Lazy} — handler might not be registered when the first event fires</li>
 *   <li>{@code @Order} — event listeners should be independent, ordering creates fragile coupling</li>
 *   <li>{@code @Primary} — meaningless for @Component beans and indicates confusion</li>
 * </ul>
 */
class IntegrationEventHandlerSpringAnnotationGuardTest {

    private static final List<Class<?>> ALL_HANDLER_CLASSES = List.of(
            SendCourseToApproveIntegrationEventHandler.class,
            CourseApprovedByAdminIntegrationEventHandler.class,
            StudentEnrolledToCourseIntegrationEventHandler.class,
            CourseRatingRecalculatedIntegrationEventHandler.class,
            UserCreatedIntegrationEventHandler.class
    );

    @Test
    void allHandlers_doNotHaveScopeAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getAnnotation(Scope.class))
                    .as("Handler %s should NOT have @Scope — must remain singleton for retry state consistency",
                            handlerClass.getSimpleName())
                    .isNull();
        }
    }

    @Test
    void allHandlers_doNotHaveLazyAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getAnnotation(Lazy.class))
                    .as("Handler %s should NOT have @Lazy — must be eagerly initialized to receive events immediately",
                            handlerClass.getSimpleName())
                    .isNull();
        }
    }

    @Test
    void allHandlers_doNotHaveOrderAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getAnnotation(Order.class))
                    .as("Handler %s should NOT have @Order — event listeners are independent and unordered",
                            handlerClass.getSimpleName())
                    .isNull();
        }
    }

    @Test
    void allHandlers_doNotHavePrimaryAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getAnnotation(Primary.class))
                    .as("Handler %s should NOT have @Primary — there is no injection ambiguity to resolve",
                            handlerClass.getSimpleName())
                    .isNull();
        }
    }

    @Test
    void allHandlers_doNotHaveDeprecatedAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getAnnotation(Deprecated.class))
                    .as("Handler %s should NOT be @Deprecated — active integration event handlers must remain operational",
                            handlerClass.getSimpleName())
                    .isNull();
        }
    }

    @Test
    void allHandlers_handlerMethod_doNotHaveOrderAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Arrays.stream(handlerClass.getDeclaredMethods())
                    .filter(m -> m.getAnnotation(org.springframework.context.event.EventListener.class) != null)
                    .forEach(method -> assertThat(method.getAnnotation(Order.class))
                            .as("@EventListener in %s should NOT have @Order — async event processing is inherently unordered",
                                    handlerClass.getSimpleName())
                            .isNull());
        }
    }

    @Test
    void allHandlers_handlerMethod_doNotHaveLazyAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Arrays.stream(handlerClass.getDeclaredMethods())
                    .filter(m -> m.getAnnotation(org.springframework.context.event.EventListener.class) != null)
                    .forEach(method -> assertThat(method.getAnnotation(Lazy.class))
                            .as("@EventListener in %s should NOT have method-level @Lazy",
                                    handlerClass.getSimpleName())
                            .isNull());
        }
    }

    @Test
    void allHandlers_noConditionalAnnotations() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            boolean hasConditional = Arrays.stream(handlerClass.getAnnotations())
                    .map(Annotation::annotationType)
                    .anyMatch(a -> a.getName().contains("Conditional"));
            assertThat(hasConditional)
                    .as("Handler %s should NOT have any @Conditional* annotation — "
                            + "event handlers must always be registered for reliable event processing",
                            handlerClass.getSimpleName())
                    .isFalse();
        }
    }

    @Test
    void allHandlers_noProfileAnnotation() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            assertThat(handlerClass.getAnnotation(org.springframework.context.annotation.Profile.class))
                    .as("Handler %s should NOT have @Profile — event handlers must be active in all profiles",
                            handlerClass.getSimpleName())
                    .isNull();
        }
    }
}
