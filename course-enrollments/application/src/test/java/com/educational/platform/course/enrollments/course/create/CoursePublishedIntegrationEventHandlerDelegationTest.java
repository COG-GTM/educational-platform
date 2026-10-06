package com.educational.platform.course.enrollments.course.create;

import com.educational.platform.courses.integration.event.CoursePublishedIntegrationEvent;

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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Pins how the published course snapshot is translated into the enrollments module command, independent of persistence.
 */
@ExtendWith(MockitoExtension.class)
public class CoursePublishedIntegrationEventHandlerDelegationTest {

    private static final UUID COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static final UUID FIRST_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440001");
    private static final UUID SECOND_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");

    @Mock
    private CreateEnrollmentCourseCommandHandler createEnrollmentCourseCommandHandler;

    @InjectMocks
    private CoursePublishedIntegrationEventHandler sut;

    @Test
    void handleCoursePublishedEvent_lecturesMappedFieldByFieldInPublishedOrder() {
        // given
        final CoursePublishedIntegrationEvent event = new CoursePublishedIntegrationEvent(COURSE, "Java Basics", List.of(
                new CoursePublishedIntegrationEvent.Lecture(SECOND_LECTURE, "Variables", 2),
                new CoursePublishedIntegrationEvent.Lecture(FIRST_LECTURE, "Intro", 1)));

        // when
        sut.handleCoursePublishedEvent(event);

        // then
        final ArgumentCaptor<CreateCourseCommand> command = ArgumentCaptor.forClass(CreateCourseCommand.class);
        verify(createEnrollmentCourseCommandHandler).handle(command.capture());
        assertThat(command.getValue().uuid()).isEqualTo(COURSE);
        assertThat(command.getValue().name()).isEqualTo("Java Basics");
        assertThat(command.getValue().lectures()).containsExactly(
                new CreateCourseCommand.CreateLectureCommand(SECOND_LECTURE, "Variables", 2),
                new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1));
    }

    @Test
    void handleCoursePublishedEvent_noLectures_commandWithEmptyLectures() {
        // given
        final CoursePublishedIntegrationEvent event = new CoursePublishedIntegrationEvent(COURSE, "Java Basics", List.of());

        // when
        sut.handleCoursePublishedEvent(event);

        // then
        final ArgumentCaptor<CreateCourseCommand> command = ArgumentCaptor.forClass(CreateCourseCommand.class);
        verify(createEnrollmentCourseCommandHandler).handle(command.capture());
        assertThat(command.getValue().uuid()).isEqualTo(COURSE);
        assertThat(command.getValue().name()).isEqualTo("Java Basics");
        assertThat(command.getValue().lectures()).isEmpty();
    }

    @Test
    void handleCoursePublishedEvent_runsAsyncAfterPublishingTransactionCommits() throws NoSuchMethodException {
        // when
        final Method handler = CoursePublishedIntegrationEventHandler.class.getMethod("handleCoursePublishedEvent", CoursePublishedIntegrationEvent.class);

        // then
        assertThat(handler.isAnnotationPresent(Async.class)).isTrue();
        final TransactionalEventListener listener = handler.getAnnotation(TransactionalEventListener.class);
        assertThat(listener).isNotNull();
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(listener.fallbackExecution()).isTrue();
    }
}
