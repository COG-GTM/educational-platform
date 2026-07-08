package com.educational.platform.courses.course.rating.update;

import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;

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
class CourseRatingRecalculatedIntegrationEventHandlerRetryTest {

    @Configuration
    @EnableRetry
    static class TestConfig {

        @Bean
        UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler() {
            return Mockito.mock(UpdateCourseRatingCommandHandler.class);
        }

        @Bean
        CourseRatingRecalculatedIntegrationEventHandler handler(UpdateCourseRatingCommandHandler commandHandler) {
            return new CourseRatingRecalculatedIntegrationEventHandler(commandHandler);
        }
    }

    @Autowired
    private CourseRatingRecalculatedIntegrationEventHandler sut;

    @Autowired
    private UpdateCourseRatingCommandHandler commandHandler;

    @BeforeEach
    void resetMock() {
        Mockito.reset(commandHandler);
    }

    @Test
    void handle_commandHandlerKeepsFailing_retriesThreeTimesThenRecovers() {
        // given
        doThrow(new RuntimeException("boom")).when(commandHandler).handle(any(UpdateCourseRatingCommand.class));
        final CourseRatingRecalculatedIntegrationEvent event =
                new CourseRatingRecalculatedIntegrationEvent(UUID.randomUUID(), 4.2);

        // when - @Recover swallows the exception after retries are exhausted
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        verify(commandHandler, times(3)).handle(any(UpdateCourseRatingCommand.class));
    }

    @Test
    void handle_transientFailure_succeedsOnSecondAttempt() {
        // given
        doThrow(new RuntimeException("transient"))
                .doNothing()
                .when(commandHandler).handle(any(UpdateCourseRatingCommand.class));
        final CourseRatingRecalculatedIntegrationEvent event =
                new CourseRatingRecalculatedIntegrationEvent(UUID.randomUUID(), 4.2);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        verify(commandHandler, times(2)).handle(any(UpdateCourseRatingCommand.class));
    }

    @Test
    void listenerMethod_isAsyncAndAfterCommit() throws NoSuchMethodException {
        final Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);

        final Async async = method.getAnnotation(Async.class);
        assertThat(async).isNotNull();
        assertThat(async.value()).isEqualTo("integrationEventExecutor");

        final TransactionalEventListener listener = method.getAnnotation(TransactionalEventListener.class);
        assertThat(listener).isNotNull();
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
