package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.config.AsyncConfig;
import com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.teacher.create.UserCreatedIntegrationEventHandler;

import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the contract between handlers' {@code @Async} annotations and
 * {@link AsyncConfig}'s executor configuration.
 * <p>
 * All handlers use {@code @Async} without specifying a bean name, which means
 * Spring resolves the executor via {@link AsyncConfigurer#getAsyncExecutor()}.
 * This test ensures that contract holds: if a handler accidentally specifies a
 * named executor that doesn't match the bean, events would execute synchronously
 * (on the caller thread) instead of asynchronously — a silent failure.
 */
class IntegrationEventHandlerAsyncExecutorContractTest {

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
                    .as("@Async in %s should use default executor (empty value) — "
                                    + "AsyncConfigurer provides the executor",
                            handlerClass.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void asyncConfig_implementsAsyncConfigurer_providingDefaultExecutor() {
        assertThat(AsyncConfigurer.class)
                .as("AsyncConfig must implement AsyncConfigurer to provide the default @Async executor")
                .isAssignableFrom(AsyncConfig.class);
    }

    @Test
    void asyncConfig_getAsyncExecutor_returnsThreadPoolTaskExecutor() {
        AsyncConfig config = new AsyncConfig();
        Executor executor = config.getAsyncExecutor();
        assertThat(executor)
                .as("getAsyncExecutor should return a ThreadPoolTaskExecutor")
                .isInstanceOf(ThreadPoolTaskExecutor.class);
    }

    @Test
    void asyncConfig_executorThreadPrefix_identifiesIntegrationEvents() {
        AsyncConfig config = new AsyncConfig();
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.getAsyncExecutor();
        assertThat(executor.getThreadNamePrefix())
                .as("Thread prefix should identify integration event processing in logs and thread dumps")
                .startsWith("integration-event");
    }

    @Test
    void asyncConfig_executorMaxPoolSize_isGreaterThanOrEqualToCorePoolSize() {
        AsyncConfig config = new AsyncConfig();
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.getAsyncExecutor();
        assertThat(executor.getMaxPoolSize())
                .as("maxPoolSize must be >= corePoolSize for valid ThreadPoolTaskExecutor config")
                .isGreaterThanOrEqualTo(executor.getCorePoolSize());
    }

    private Method findEventListenerMethod(Class<?> handlerClass) {
        return Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> m.getAnnotation(EventListener.class) != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No @EventListener method found in " + handlerClass.getSimpleName()));
    }
}
