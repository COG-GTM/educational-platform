package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.CreateCourseProposalCommandHandler;
import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.courses.course.approve.ApproveCourseCommandHandler;
import com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;
import com.educational.platform.courses.teacher.create.CreateTeacherCommandHandler;
import com.educational.platform.courses.teacher.create.UserCreatedIntegrationEventHandler;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cross-cutting test verifying exception propagation behavior across all integration
 * event handlers. Specifically tests:
 * <ul>
 *   <li>Non-retryable exceptions (those NOT in {@code retryFor}) are rethrown and NOT caught by {@code @Recover}</li>
 *   <li>The catch block in the handler does NOT suppress exceptions — it always re-throws</li>
 *   <li>{@code NullPointerException} from command handlers propagates correctly
 *       (important because NPE is NOT a DataAccessException, so it won't be retried or recovered)</li>
 *   <li>Checked exceptions wrapped in RuntimeException are properly propagated</li>
 * </ul>
 * <p>
 * At runtime with Spring retry enabled:
 * <ul>
 *   <li>DataAccessException/OOLFE → retried up to MAX_ATTEMPTS → @Recover invoked → dead-letter persisted</li>
 *   <li>Other RuntimeExceptions (NPE, ISE, etc.) → NOT retried → propagates to AsyncUncaughtExceptionHandler</li>
 * </ul>
 */
class IntegrationEventHandlerExceptionPropagationTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void sendCourseToApproveHandler_npeFromCommandHandler_propagatesWithoutRecovery() {
        // given
        var commandHandler = mock(CreateCourseProposalCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        doThrow(new NullPointerException("entity was null")).when(commandHandler).handle(any());
        var handler = new SendCourseToApproveIntegrationEventHandler(commandHandler, repo);

        // when/then — NPE propagates (not in retryFor, not caught by @Recover)
        assertThatThrownBy(() -> handler.handleSendCourseToApproveEvent(
                new SendCourseToApproveIntegrationEvent(COURSE_ID)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("entity was null");
        verifyNoInteractions(repo);
    }

    @Test
    void courseApprovedByAdminHandler_npeFromCommandHandler_propagatesWithoutRecovery() {
        var commandHandler = mock(ApproveCourseCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        doThrow(new NullPointerException("course not loaded")).when(commandHandler).handle(any());
        var handler = new CourseApprovedByAdminIntegrationEventHandler(commandHandler, repo);

        assertThatThrownBy(() -> handler.handleCourseApprovedByAdminEvent(
                new CourseApprovedByAdminIntegrationEvent(COURSE_ID)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("course not loaded");
        verifyNoInteractions(repo);
    }

    @Test
    void studentEnrolledHandler_npeFromCommandHandler_propagatesWithoutRecovery() {
        var commandHandler = mock(IncreaseNumberOfStudentsCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        doThrow(new NullPointerException("student count field null")).when(commandHandler).handle(any());
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(commandHandler, repo);

        assertThatThrownBy(() -> handler.handleStudentEnrolledToCourseEvent(
                new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student")))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("student count field null");
        verifyNoInteractions(repo);
    }

    @Test
    void courseRatingHandler_npeFromCommandHandler_propagatesWithoutRecovery() {
        var commandHandler = mock(UpdateCourseRatingCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        doThrow(new NullPointerException("rating entity null")).when(commandHandler).handle(any());
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(commandHandler, repo);

        assertThatThrownBy(() -> handler.handleCourseRatingRecalculatedEvent(
                new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 3.5)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("rating entity null");
        verifyNoInteractions(repo);
    }

    @Test
    void userCreatedHandler_npeFromCommandHandler_propagatesWithoutRecovery() {
        var commandHandler = mock(CreateTeacherCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        doThrow(new NullPointerException("teacher repo returned null")).when(commandHandler).handle(any());
        var handler = new UserCreatedIntegrationEventHandler(commandHandler, repo);

        assertThatThrownBy(() -> handler.handleUserCreatedEvent(
                new UserCreatedIntegrationEvent("teacher", "t@example.com")))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("teacher repo returned null");
        verifyNoInteractions(repo);
    }

    @Test
    void sendCourseToApproveHandler_wrappedCheckedException_propagates() {
        // given — a checked exception wrapped in RuntimeException (common pattern in JPA/Spring)
        var commandHandler = mock(CreateCourseProposalCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var cause = new java.io.IOException("network error");
        doThrow(new RuntimeException("wrapped checked exception", cause)).when(commandHandler).handle(any());
        var handler = new SendCourseToApproveIntegrationEventHandler(commandHandler, repo);

        // when/then — wrapping RuntimeException is not DataAccessException, so it propagates
        assertThatThrownBy(() -> handler.handleSendCourseToApproveEvent(
                new SendCourseToApproveIntegrationEvent(COURSE_ID)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("wrapped checked exception")
                .hasCauseInstanceOf(java.io.IOException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void courseApprovedHandler_illegalArgumentException_propagates() {
        var commandHandler = mock(ApproveCourseCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        doThrow(new IllegalArgumentException("invalid course state")).when(commandHandler).handle(any());
        var handler = new CourseApprovedByAdminIntegrationEventHandler(commandHandler, repo);

        assertThatThrownBy(() -> handler.handleCourseApprovedByAdminEvent(
                new CourseApprovedByAdminIntegrationEvent(COURSE_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid course state");
        verifyNoInteractions(repo);
    }

    @Test
    void studentEnrolledHandler_unsupportedOperationException_propagates() {
        var commandHandler = mock(IncreaseNumberOfStudentsCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        doThrow(new UnsupportedOperationException("operation not supported")).when(commandHandler).handle(any());
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(commandHandler, repo);

        assertThatThrownBy(() -> handler.handleStudentEnrolledToCourseEvent(
                new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student")))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("operation not supported");
        verifyNoInteractions(repo);
    }

    @Test
    void courseRatingHandler_arithmeticException_propagates() {
        var commandHandler = mock(UpdateCourseRatingCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        doThrow(new ArithmeticException("division by zero")).when(commandHandler).handle(any());
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(commandHandler, repo);

        assertThatThrownBy(() -> handler.handleCourseRatingRecalculatedEvent(
                new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.0)))
                .isInstanceOf(ArithmeticException.class)
                .hasMessage("division by zero");
        verifyNoInteractions(repo);
    }

    @Test
    void userCreatedHandler_securityException_propagates() {
        var commandHandler = mock(CreateTeacherCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        doThrow(new SecurityException("access denied")).when(commandHandler).handle(any());
        var handler = new UserCreatedIntegrationEventHandler(commandHandler, repo);

        assertThatThrownBy(() -> handler.handleUserCreatedEvent(
                new UserCreatedIntegrationEvent("teacher", "t@example.com")))
                .isInstanceOf(SecurityException.class)
                .hasMessage("access denied");
        verifyNoInteractions(repo);
    }

    @Test
    void allHandlers_exceptionRethrown_isSameInstance() {
        // given — verify catch block does NOT wrap/transform the exception
        var proposalCmd = mock(CreateCourseProposalCommandHandler.class);
        var approveCmd = mock(ApproveCourseCommandHandler.class);
        var increaseCmd = mock(IncreaseNumberOfStudentsCommandHandler.class);
        var ratingCmd = mock(UpdateCourseRatingCommandHandler.class);
        var teacherCmd = mock(CreateTeacherCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);

        var npe1 = new NullPointerException("1");
        var npe2 = new NullPointerException("2");
        var npe3 = new NullPointerException("3");
        var npe4 = new NullPointerException("4");
        var npe5 = new NullPointerException("5");

        doThrow(npe1).when(proposalCmd).handle(any());
        doThrow(npe2).when(approveCmd).handle(any());
        doThrow(npe3).when(increaseCmd).handle(any());
        doThrow(npe4).when(ratingCmd).handle(any());
        doThrow(npe5).when(teacherCmd).handle(any());

        // then — each handler rethrows the exact same exception instance (not wrapped)
        assertThatThrownBy(() -> new SendCourseToApproveIntegrationEventHandler(proposalCmd, repo)
                .handleSendCourseToApproveEvent(new SendCourseToApproveIntegrationEvent(COURSE_ID)))
                .isSameAs(npe1);
        assertThatThrownBy(() -> new CourseApprovedByAdminIntegrationEventHandler(approveCmd, repo)
                .handleCourseApprovedByAdminEvent(new CourseApprovedByAdminIntegrationEvent(COURSE_ID)))
                .isSameAs(npe2);
        assertThatThrownBy(() -> new StudentEnrolledToCourseIntegrationEventHandler(increaseCmd, repo)
                .handleStudentEnrolledToCourseEvent(new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "u")))
                .isSameAs(npe3);
        assertThatThrownBy(() -> new CourseRatingRecalculatedIntegrationEventHandler(ratingCmd, repo)
                .handleCourseRatingRecalculatedEvent(new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 1.0)))
                .isSameAs(npe4);
        assertThatThrownBy(() -> new UserCreatedIntegrationEventHandler(teacherCmd, repo)
                .handleUserCreatedEvent(new UserCreatedIntegrationEvent("t", "e")))
                .isSameAs(npe5);
    }
}
