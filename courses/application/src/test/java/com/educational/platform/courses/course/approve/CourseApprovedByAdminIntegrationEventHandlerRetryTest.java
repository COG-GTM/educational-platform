package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig
class CourseApprovedByAdminIntegrationEventHandlerRetryTest {

    @Configuration
    @EnableRetry
    static class TestConfig {

        @Bean
        ApproveCourseCommandHandler approveCourseCommandHandler() {
            return Mockito.mock(ApproveCourseCommandHandler.class);
        }

        @Bean
        CourseApprovedByAdminIntegrationEventHandler handler(ApproveCourseCommandHandler commandHandler) {
            return new CourseApprovedByAdminIntegrationEventHandler(commandHandler);
        }
    }

    @Autowired
    private CourseApprovedByAdminIntegrationEventHandler sut;

    @Autowired
    private ApproveCourseCommandHandler commandHandler;

    @BeforeEach
    void resetMock() {
        Mockito.reset(commandHandler);
    }

    @Test
    void handle_commandHandlerKeepsFailing_retriesThreeTimesThenRecovers() {
        // given
        doThrow(new RuntimeException("boom")).when(commandHandler).handle(any(ApproveCourseCommand.class));
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID());

        // when - @Recover swallows the exception after retries are exhausted
        sut.handleCourseApprovedByAdminEvent(event);

        // then
        verify(commandHandler, times(3)).handle(any(ApproveCourseCommand.class));
    }

    @Test
    void handle_transientFailure_succeedsOnSecondAttempt() {
        // given
        doThrow(new RuntimeException("transient"))
                .doNothing()
                .when(commandHandler).handle(any(ApproveCourseCommand.class));
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID());

        // when
        sut.handleCourseApprovedByAdminEvent(event);

        // then
        verify(commandHandler, times(2)).handle(any(ApproveCourseCommand.class));
    }

    @Test
    void listenerMethod_isAsyncAndAfterCommit() throws NoSuchMethodException {
        final Method method = CourseApprovedByAdminIntegrationEventHandler.class
                .getMethod("handleCourseApprovedByAdminEvent", CourseApprovedByAdminIntegrationEvent.class);

        final Async async = method.getAnnotation(Async.class);
        assertThat(async).isNotNull();
        assertThat(async.value()).isEqualTo("integrationEventExecutor");

        final TransactionalEventListener listener = method.getAnnotation(TransactionalEventListener.class);
        assertThat(listener).isNotNull();
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
