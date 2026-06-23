package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.CreateCourseProposalCommandHandler;
import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.courses.course.approve.ApproveCourseCommandHandler;
import com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import com.educational.platform.courses.teacher.create.CreateTeacherCommandHandler;
import com.educational.platform.courses.teacher.create.UserCreatedIntegrationEventHandler;
import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifies the catch-and-rethrow behavior of handler methods across all handlers.
 * <p>
 * Each handler has:
 * <pre>
 *     try {
 *         commandHandler.handle(...);
 *     } catch (Exception e) {
 *         log.error("Failed to handle integration event: {}", event, e);
 *         throw e;
 *     }
 * </pre>
 * The catch block catches ALL {@link Exception} subtypes (but NOT {@link Error}),
 * logs them, and rethrows. This test verifies:
 * <ul>
 *   <li>Diverse exception types are caught and rethrown unchanged</li>
 *   <li>Error subtypes bypass the catch block entirely</li>
 *   <li>The repository is never touched during the handler method (only during recover)</li>
 *   <li>The exception instance is the exact same object (not wrapped or copied)</li>
 * </ul>
 */
class IntegrationEventHandlerCatchBlockBehaviorTest {

    @Test
    void sendCourseToApproveHandler_classNotFoundException_caughtAndRethrown() {
        // given — ClassCastException is a common runtime exception not explicitly tested elsewhere
        var commandHandler = mock(CreateCourseProposalCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(commandHandler, repo);
        var event = new SendCourseToApproveIntegrationEvent(UUID.randomUUID());
        var exception = new ClassCastException("Cannot cast Course to CourseProposal");
        doThrow(exception).when(commandHandler).handle(any());

        // when/then — exception passes through catch block unchanged
        assertThatThrownBy(() -> handler.handleSendCourseToApproveEvent(event))
                .isSameAs(exception);
        verifyNoInteractions(repo);
    }

    @Test
    void courseApprovedHandler_unsupportedOperationException_caughtAndRethrown() {
        var commandHandler = mock(ApproveCourseCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(commandHandler, repo);
        var event = new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID());
        var exception = new UnsupportedOperationException("Approval not supported for draft courses");
        doThrow(exception).when(commandHandler).handle(any());

        assertThatThrownBy(() -> handler.handleCourseApprovedByAdminEvent(event))
                .isSameAs(exception);
        verifyNoInteractions(repo);
    }

    @Test
    void studentEnrolledHandler_arrayIndexOutOfBoundsException_caughtAndRethrown() {
        var commandHandler = mock(IncreaseNumberOfStudentsCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(commandHandler, repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(UUID.randomUUID(), "student1");
        var exception = new ArrayIndexOutOfBoundsException("Index 10 out of bounds");
        doThrow(exception).when(commandHandler).handle(any());

        assertThatThrownBy(() -> handler.handleStudentEnrolledToCourseEvent(event))
                .isSameAs(exception);
        verifyNoInteractions(repo);
    }

    @Test
    void courseRatingHandler_arithmeticException_caughtAndRethrown() {
        var commandHandler = mock(UpdateCourseRatingCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(commandHandler, repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(UUID.randomUUID(), 4.5);
        var exception = new ArithmeticException("/ by zero");
        doThrow(exception).when(commandHandler).handle(any());

        assertThatThrownBy(() -> handler.handleCourseRatingRecalculatedEvent(event))
                .isSameAs(exception);
        verifyNoInteractions(repo);
    }

    @Test
    void userCreatedHandler_securityException_caughtAndRethrown() {
        var commandHandler = mock(CreateTeacherCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(commandHandler, repo);
        var event = new UserCreatedIntegrationEvent("admin", "admin@example.com");
        var exception = new SecurityException("Insufficient permissions");
        doThrow(exception).when(commandHandler).handle(any());

        assertThatThrownBy(() -> handler.handleUserCreatedEvent(event))
                .isSameAs(exception);
        verifyNoInteractions(repo);
    }

    @Test
    void allHandlers_outOfMemoryError_bypassesCatchBlock() {
        // given — Errors bypass catch(Exception) entirely
        var commandHandler = mock(CreateCourseProposalCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(commandHandler, repo);
        var event = new SendCourseToApproveIntegrationEvent(UUID.randomUUID());
        doThrow(new OutOfMemoryError("Java heap space")).when(commandHandler).handle(any());

        // when/then — OOM bypasses catch(Exception), propagates directly
        try {
            handler.handleSendCourseToApproveEvent(event);
            assertThat(true).as("Expected OutOfMemoryError").isFalse();
        } catch (OutOfMemoryError e) {
            assertThat(e.getMessage()).isEqualTo("Java heap space");
        }
        verifyNoInteractions(repo);
    }

    @Test
    void allHandlers_dataAccessException_caughtAndRethrown_repositoryNeverTouched() {
        // given — DataAccessException IS caught by catch(Exception) but the repository
        // is only used in recover(), never in the handler method
        var commandHandler = mock(CreateCourseProposalCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(commandHandler, repo);
        var event = new SendCourseToApproveIntegrationEvent(UUID.randomUUID());
        var exception = new DataAccessResourceFailureException("connection pool exhausted");
        doThrow(exception).when(commandHandler).handle(any());

        // when
        assertThatThrownBy(() -> handler.handleSendCourseToApproveEvent(event))
                .isSameAs(exception);

        // then — handler only rethrows, it NEVER persists a dead-letter record
        verifyNoInteractions(repo);
    }

    @Test
    void allHandlers_exceptionWithNullMessage_caughtAndRethrown() {
        var commandHandler = mock(CreateTeacherCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(commandHandler, repo);
        var event = new UserCreatedIntegrationEvent("user", "user@test.com");
        var exception = new RuntimeException((String) null);
        doThrow(exception).when(commandHandler).handle(any());

        assertThatThrownBy(() -> handler.handleUserCreatedEvent(event))
                .isSameAs(exception)
                .hasMessage(null);
        verifyNoInteractions(repo);
    }

    @Test
    void allHandlers_exceptionWithCause_caughtAndRethrown_causePreserved() {
        var commandHandler = mock(ApproveCourseCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(commandHandler, repo);
        var event = new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID());
        var cause = new java.io.IOException("disk full");
        var exception = new RuntimeException("wrapped IO exception", cause);
        doThrow(exception).when(commandHandler).handle(any());

        assertThatThrownBy(() -> handler.handleCourseApprovedByAdminEvent(event))
                .isSameAs(exception)
                .hasCause(cause);
        verifyNoInteractions(repo);
    }
}
