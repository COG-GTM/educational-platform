package com.educational.platform.courses.course.rating.update;

import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CourseRatingRecalculatedIntegrationEventHandlerTest {

    @Mock
    private CourseRatingRecalculatedRetryableInvoker courseRatingRecalculatedRetryableInvoker;

    @InjectMocks
    private CourseRatingRecalculatedIntegrationEventHandler sut;


    @Test
    void handleCourseRatingRecalculatedEvent_updateCourseRatingCommandExecuted() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        verify(courseRatingRecalculatedRetryableInvoker).invoke(event);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_isAsyncOnIntegrationEventExecutorAndListensAfterCommit() throws Exception {
        // given
        final Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);

        // then
        final Async async = method.getAnnotation(Async.class);
        assertThat(async).isNotNull();
        assertThat(async.value()).isEqualTo("integrationEventExecutor");

        final TransactionalEventListener listener = method.getAnnotation(TransactionalEventListener.class);
        assertThat(listener).isNotNull();
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(listener.fallbackExecution()).isFalse();
    }

}
