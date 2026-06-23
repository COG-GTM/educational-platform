package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.teacher.create.UserCreatedIntegrationEventHandler;

import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the annotation declaration order on handler methods is consistent
 * across all 5 integration event handlers: @Async, @Retryable, @EventListener.
 * <p>
 * While Java annotation ordering has no semantic meaning, consistent ordering
 * improves readability and prevents accidental omission during copy-paste.
 * This test also verifies that all three annotations are co-present.
 */
class IntegrationEventHandlerAnnotationOrderTest {

    private static final List<Class<?>> ALL_HANDLER_CLASSES = List.of(
            SendCourseToApproveIntegrationEventHandler.class,
            CourseApprovedByAdminIntegrationEventHandler.class,
            StudentEnrolledToCourseIntegrationEventHandler.class,
            CourseRatingRecalculatedIntegrationEventHandler.class,
            UserCreatedIntegrationEventHandler.class
    );

    @Test
    void allHandlers_eventListenerMethod_hasAllThreeAnnotations() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            assertThat(method.getAnnotation(Async.class))
                    .as("@Async missing on handler method in %s", handlerClass.getSimpleName())
                    .isNotNull();
            assertThat(method.getAnnotation(Retryable.class))
                    .as("@Retryable missing on handler method in %s", handlerClass.getSimpleName())
                    .isNotNull();
            assertThat(method.getAnnotation(EventListener.class))
                    .as("@EventListener missing on handler method in %s", handlerClass.getSimpleName())
                    .isNotNull();
        }
    }

    @Test
    void allHandlers_eventListenerMethod_annotationOrderIsConsistent() {
        // Collect the annotation order for each handler and verify they're all the same
        List<List<String>> annotationOrders = ALL_HANDLER_CLASSES.stream()
                .map(this::findEventListenerMethod)
                .map(this::getRelevantAnnotationOrder)
                .toList();

        List<String> expected = annotationOrders.getFirst();
        for (int i = 1; i < annotationOrders.size(); i++) {
            assertThat(annotationOrders.get(i))
                    .as("Annotation order in %s differs from %s",
                            ALL_HANDLER_CLASSES.get(i).getSimpleName(),
                            ALL_HANDLER_CLASSES.getFirst().getSimpleName())
                    .isEqualTo(expected);
        }
    }

    @Test
    void allHandlers_eventListenerMethod_asyncAnnotationIsDeclaredFirst() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Annotation[] annotations = method.getDeclaredAnnotations();
            assertThat(annotations[0].annotationType())
                    .as("First annotation on handler method in %s should be @Async", handlerClass.getSimpleName())
                    .isEqualTo(Async.class);
        }
    }

    @Test
    void allHandlers_eventListenerMethod_eventListenerAnnotationIsDeclaredLast() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Annotation[] annotations = method.getDeclaredAnnotations();
            Annotation last = annotations[annotations.length - 1];
            assertThat(last.annotationType())
                    .as("Last annotation on handler method in %s should be @EventListener", handlerClass.getSimpleName())
                    .isEqualTo(EventListener.class);
        }
    }

    @Test
    void allHandlers_eventListenerMethod_retryableAnnotationIsBetweenAsyncAndEventListener() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Annotation[] annotations = method.getDeclaredAnnotations();
            assertThat(annotations.length)
                    .as("Handler method in %s should have exactly 3 annotations", handlerClass.getSimpleName())
                    .isEqualTo(3);
            assertThat(annotations[1].annotationType())
                    .as("Second annotation on handler method in %s should be @Retryable", handlerClass.getSimpleName())
                    .isEqualTo(Retryable.class);
        }
    }

    private List<String> getRelevantAnnotationOrder(Method method) {
        return Arrays.stream(method.getDeclaredAnnotations())
                .map(a -> a.annotationType().getSimpleName())
                .filter(name -> name.equals("Async") || name.equals("Retryable") || name.equals("EventListener"))
                .toList();
    }

    private Method findEventListenerMethod(Class<?> handlerClass) {
        return Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> m.getAnnotation(EventListener.class) != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No @EventListener method found in " + handlerClass.getSimpleName()));
    }
}
