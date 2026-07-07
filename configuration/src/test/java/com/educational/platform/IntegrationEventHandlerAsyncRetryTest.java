package com.educational.platform;

import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Verifies that retry proxying composes with the async executor configuration:
 * a handler invoked through the proxy runs on the integration event executor
 * and is retried there until attempts are exhausted.
 */
public class IntegrationEventHandlerAsyncRetryTest {

    @EnableAsync(order = Ordered.LOWEST_PRECEDENCE - 1)
    @EnableRetry(order = Ordered.LOWEST_PRECEDENCE)
    @Configuration
    static class AsyncRetryConfig {
    }

    @Test
    void handleStudentEnrolledToCourseEvent_commandHandlerAlwaysFails_retriedOnIntegrationEventExecutor() throws InterruptedException {
        final IncreaseNumberOfStudentsCommandHandler commandHandler = mock(IncreaseNumberOfStudentsCommandHandler.class);
        final CountDownLatch attempts = new CountDownLatch(3);
        final CountDownLatch ranOnExecutorThread = new CountDownLatch(1);
        doAnswer(invocation -> {
            if (Thread.currentThread().getName().startsWith(AsyncConfig.THREAD_NAME_PREFIX)) {
                ranOnExecutorThread.countDown();
            }
            attempts.countDown();
            throw new RuntimeException("boom");
        }).when(commandHandler).handle(any());

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(AsyncConfig.class, AsyncRetryConfig.class);
            context.registerBean(IncreaseNumberOfStudentsCommandHandler.class, () -> commandHandler);
            context.registerBean(StudentEnrolledToCourseIntegrationEventHandler.class);
            context.refresh();

            final StudentEnrolledToCourseIntegrationEventHandler sut = context.getBean(StudentEnrolledToCourseIntegrationEventHandler.class);
            assertThat(AopUtils.isAopProxy(sut)).isTrue();

            sut.handleStudentEnrolledToCourseEvent(new StudentEnrolledToCourseIntegrationEvent(UUID.randomUUID(), "username"));

            assertThat(attempts.await(10, TimeUnit.SECONDS))
                    .as("command handler should be invoked 3 times before retries are exhausted")
                    .isTrue();
            assertThat(ranOnExecutorThread.await(10, TimeUnit.SECONDS))
                    .as("handler should run on the integrationEventExecutor thread")
                    .isTrue();
        }
    }

}
