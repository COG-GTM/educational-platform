package com.educational.platform.course.reviews.enrollment.create;

import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
public class StudentEnrolledToCourseIntegrationEventHandlerTest {

    @Mock
    private CreateReviewerEnrollmentCommandHandler createReviewerEnrollmentCommandHandler;

    @InjectMocks
    private StudentEnrolledToCourseIntegrationEventHandler sut;

    @Test
    void handleStudentEnrolledToCourseEvent_createReviewerEnrollmentCommandExecuted() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        final ArgumentCaptor<CreateReviewerEnrollmentCommand> argument = ArgumentCaptor.forClass(CreateReviewerEnrollmentCommand.class);
        verify(createReviewerEnrollmentCommandHandler).handle(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("courseId", uuid)
                .hasFieldOrPropertyWithValue("username", "username");
    }

    @Test
    void handleStudentEnrolledToCourseEvent_isAsyncAfterCommitListenerWithFallback() throws NoSuchMethodException {
        // given
        final Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);

        // when
        final TransactionalEventListener listener = method.getAnnotation(TransactionalEventListener.class);

        // then
        assertThat(method.isAnnotationPresent(Async.class)).isTrue();
        assertThat(listener).isNotNull();
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(listener.fallbackExecution()).isTrue();
    }

}
