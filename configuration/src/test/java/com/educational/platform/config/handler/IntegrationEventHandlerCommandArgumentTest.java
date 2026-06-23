package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import com.educational.platform.administration.course.create.CreateCourseProposalCommandHandler;
import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.courses.course.approve.ApproveCourseCommand;
import com.educational.platform.courses.course.approve.ApproveCourseCommandHandler;
import com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommand;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import com.educational.platform.courses.teacher.create.CreateTeacherCommandHandler;
import com.educational.platform.courses.teacher.create.UserCreatedIntegrationEventHandler;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Cross-cutting test verifying that each integration event handler correctly extracts
 * the appropriate fields from the event and passes them to the corresponding command.
 * <p>
 * This guards against:
 * <ul>
 *   <li>Accidentally swapping event fields during construction (e.g., passing username where courseId is expected)</li>
 *   <li>Regression if event record fields are reordered</li>
 *   <li>Incorrect field selection for multi-field events (e.g., StudentEnrolledToCourseIntegrationEvent has courseId AND username)</li>
 * </ul>
 */
class IntegrationEventHandlerCommandArgumentTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void sendCourseToApproveHandler_passesCourseIdToCommand() {
        // given
        var commandHandler = mock(CreateCourseProposalCommandHandler.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                commandHandler, mock(FailedIntegrationEventRepository.class));
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then
        var captor = ArgumentCaptor.forClass(CreateCourseProposalCommand.class);
        verify(commandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("uuid", COURSE_ID);
    }

    @Test
    void courseApprovedByAdminHandler_passesCourseIdToCommand() {
        // given
        var commandHandler = mock(ApproveCourseCommandHandler.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                commandHandler, mock(FailedIntegrationEventRepository.class));
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);

        // when
        handler.handleCourseApprovedByAdminEvent(event);

        // then
        var captor = ArgumentCaptor.forClass(ApproveCourseCommand.class);
        verify(commandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("uuid", COURSE_ID);
    }

    @Test
    void studentEnrolledHandler_passesCourseIdToCommand_ignoresUsername() {
        // given — event has both courseId and username, but handler only uses courseId
        var commandHandler = mock(IncreaseNumberOfStudentsCommandHandler.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                commandHandler, mock(FailedIntegrationEventRepository.class));
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student@example.com");

        // when
        handler.handleStudentEnrolledToCourseEvent(event);

        // then — only courseId is extracted; username is not passed to command
        var captor = ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(commandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("uuid", COURSE_ID);
    }

    @Test
    void courseRatingRecalculatedHandler_passesBothCourseIdAndRatingToCommand() {
        // given — event has courseId and rating, both used in the command
        var commandHandler = mock(UpdateCourseRatingCommandHandler.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                commandHandler, mock(FailedIntegrationEventRepository.class));
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.75);

        // when
        handler.handleCourseRatingRecalculatedEvent(event);

        // then
        var captor = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(commandHandler).handle(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("uuid", COURSE_ID)
                .hasFieldOrPropertyWithValue("rating", 4.75);
    }

    @Test
    void userCreatedHandler_passesUsernameToCommand_ignoresEmail() {
        // given — event has both username and email, but handler only uses username
        var commandHandler = mock(CreateTeacherCommandHandler.class);
        var handler = new UserCreatedIntegrationEventHandler(
                commandHandler, mock(FailedIntegrationEventRepository.class));
        var event = new UserCreatedIntegrationEvent("johndoe", "john@example.com");

        // when
        handler.handleUserCreatedEvent(event);

        // then — only username is extracted; email is not passed to command
        var captor = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(commandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("username", "johndoe");
    }

    @Test
    void studentEnrolledHandler_differentUsername_commandStillReceivesSameCourseId() {
        // given — verify the handler extracts courseId regardless of username value
        var commandHandler = mock(IncreaseNumberOfStudentsCommandHandler.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                commandHandler, mock(FailedIntegrationEventRepository.class));

        var event1 = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "alice");
        var event2 = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "bob");

        // when
        handler.handleStudentEnrolledToCourseEvent(event1);
        handler.handleStudentEnrolledToCourseEvent(event2);

        // then — both invocations pass the same courseId
        var captor = ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(commandHandler, times(2)).handle(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(cmd ->
                assertThat(cmd).hasFieldOrPropertyWithValue("uuid", COURSE_ID));
    }

    @Test
    void userCreatedHandler_differentEmail_commandStillReceivesSameUsername() {
        // given — verify the handler extracts username regardless of email value
        var commandHandler = mock(CreateTeacherCommandHandler.class);
        var handler = new UserCreatedIntegrationEventHandler(
                commandHandler, mock(FailedIntegrationEventRepository.class));

        var event1 = new UserCreatedIntegrationEvent("teacher1", "old@example.com");
        var event2 = new UserCreatedIntegrationEvent("teacher1", "new@example.com");

        // when
        handler.handleUserCreatedEvent(event1);
        handler.handleUserCreatedEvent(event2);

        // then — both invocations pass "teacher1" regardless of email
        var captor = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(commandHandler, times(2)).handle(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(cmd ->
                assertThat(cmd).hasFieldOrPropertyWithValue("username", "teacher1"));
    }

    @Test
    void courseRatingRecalculatedHandler_extremeRatingValues_passedUnmodified() {
        // given — verify edge case ratings are not clamped or transformed
        var commandHandler = mock(UpdateCourseRatingCommandHandler.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                commandHandler, mock(FailedIntegrationEventRepository.class));

        handler.handleCourseRatingRecalculatedEvent(
                new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 0.0));
        handler.handleCourseRatingRecalculatedEvent(
                new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, -1.5));
        handler.handleCourseRatingRecalculatedEvent(
                new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, Double.MAX_VALUE));

        // then
        var captor = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(commandHandler, times(3)).handle(captor.capture());
        assertThat(captor.getAllValues().get(0)).hasFieldOrPropertyWithValue("rating", 0.0);
        assertThat(captor.getAllValues().get(1)).hasFieldOrPropertyWithValue("rating", -1.5);
        assertThat(captor.getAllValues().get(2)).hasFieldOrPropertyWithValue("rating", Double.MAX_VALUE);
    }

    @Test
    void allHandlers_commandHandlerInvokedExactlyOncePerEvent() {
        // given
        var proposalHandler = mock(CreateCourseProposalCommandHandler.class);
        var approveHandler = mock(ApproveCourseCommandHandler.class);
        var increaseHandler = mock(IncreaseNumberOfStudentsCommandHandler.class);
        var ratingHandler = mock(UpdateCourseRatingCommandHandler.class);
        var teacherHandler = mock(CreateTeacherCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);

        // when
        new SendCourseToApproveIntegrationEventHandler(proposalHandler, repo)
                .handleSendCourseToApproveEvent(new SendCourseToApproveIntegrationEvent(COURSE_ID));
        new CourseApprovedByAdminIntegrationEventHandler(approveHandler, repo)
                .handleCourseApprovedByAdminEvent(new CourseApprovedByAdminIntegrationEvent(COURSE_ID));
        new StudentEnrolledToCourseIntegrationEventHandler(increaseHandler, repo)
                .handleStudentEnrolledToCourseEvent(new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "user"));
        new CourseRatingRecalculatedIntegrationEventHandler(ratingHandler, repo)
                .handleCourseRatingRecalculatedEvent(new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.0));
        new UserCreatedIntegrationEventHandler(teacherHandler, repo)
                .handleUserCreatedEvent(new UserCreatedIntegrationEvent("teacher", "t@email.com"));

        // then — each command handler invoked exactly once
        verify(proposalHandler, times(1)).handle(any());
        verify(approveHandler, times(1)).handle(any());
        verify(increaseHandler, times(1)).handle(any());
        verify(ratingHandler, times(1)).handle(any());
        verify(teacherHandler, times(1)).handle(any());
    }
}
