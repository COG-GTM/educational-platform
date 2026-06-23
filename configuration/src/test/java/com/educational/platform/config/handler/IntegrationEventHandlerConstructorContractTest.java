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
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Verifies constructor null-safety contracts for all integration event handlers.
 * Handlers accept null dependencies at construction time (no defensive null-checks),
 * but fail-fast with NullPointerException when the null dependency is actually used.
 */
class IntegrationEventHandlerConstructorContractTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    // --- SendCourseToApproveIntegrationEventHandler ---

    @Test
    void sendCourseToApproveHandler_constructorWithNullCommandHandler_instantiatesSuccessfully() {
        var handler = new SendCourseToApproveIntegrationEventHandler(
                null, mock(FailedIntegrationEventRepository.class));
        assertThat(handler).isNotNull();
    }

    @Test
    void sendCourseToApproveHandler_handleWithNullCommandHandler_throwsNullPointerException() {
        var handler = new SendCourseToApproveIntegrationEventHandler(
                null, mock(FailedIntegrationEventRepository.class));
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        assertThatThrownBy(() -> handler.handleSendCourseToApproveEvent(event))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void sendCourseToApproveHandler_recoverWithNullRepository_throwsNullPointerException() {
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), null);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var exception = new DataAccessResourceFailureException("DB error");
        assertThatThrownBy(() -> handler.recover(exception, event))
                .isInstanceOf(NullPointerException.class);
    }

    // --- CourseApprovedByAdminIntegrationEventHandler ---

    @Test
    void courseApprovedHandler_constructorWithNullCommandHandler_instantiatesSuccessfully() {
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                null, mock(FailedIntegrationEventRepository.class));
        assertThat(handler).isNotNull();
    }

    @Test
    void courseApprovedHandler_handleWithNullCommandHandler_throwsNullPointerException() {
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                null, mock(FailedIntegrationEventRepository.class));
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        assertThatThrownBy(() -> handler.handleCourseApprovedByAdminEvent(event))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void courseApprovedHandler_recoverWithNullRepository_throwsNullPointerException() {
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), null);
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        var exception = new DataAccessResourceFailureException("DB error");
        assertThatThrownBy(() -> handler.recover(exception, event))
                .isInstanceOf(NullPointerException.class);
    }

    // --- StudentEnrolledToCourseIntegrationEventHandler ---

    @Test
    void studentEnrolledHandler_constructorWithNullCommandHandler_instantiatesSuccessfully() {
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                null, mock(FailedIntegrationEventRepository.class));
        assertThat(handler).isNotNull();
    }

    @Test
    void studentEnrolledHandler_handleWithNullCommandHandler_throwsNullPointerException() {
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                null, mock(FailedIntegrationEventRepository.class));
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        assertThatThrownBy(() -> handler.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void studentEnrolledHandler_recoverWithNullRepository_throwsNullPointerException() {
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), null);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        var exception = new DataAccessResourceFailureException("DB error");
        assertThatThrownBy(() -> handler.recover(exception, event))
                .isInstanceOf(NullPointerException.class);
    }

    // --- CourseRatingRecalculatedIntegrationEventHandler ---

    @Test
    void courseRatingHandler_constructorWithNullCommandHandler_instantiatesSuccessfully() {
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                null, mock(FailedIntegrationEventRepository.class));
        assertThat(handler).isNotNull();
    }

    @Test
    void courseRatingHandler_handleWithNullCommandHandler_throwsNullPointerException() {
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                null, mock(FailedIntegrationEventRepository.class));
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        assertThatThrownBy(() -> handler.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void courseRatingHandler_recoverWithNullRepository_throwsNullPointerException() {
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), null);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        var exception = new DataAccessResourceFailureException("DB error");
        assertThatThrownBy(() -> handler.recover(exception, event))
                .isInstanceOf(NullPointerException.class);
    }

    // --- UserCreatedIntegrationEventHandler ---

    @Test
    void userCreatedHandler_constructorWithNullCommandHandler_instantiatesSuccessfully() {
        var handler = new UserCreatedIntegrationEventHandler(
                null, mock(FailedIntegrationEventRepository.class));
        assertThat(handler).isNotNull();
    }

    @Test
    void userCreatedHandler_handleWithNullCommandHandler_throwsNullPointerException() {
        var handler = new UserCreatedIntegrationEventHandler(
                null, mock(FailedIntegrationEventRepository.class));
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        assertThatThrownBy(() -> handler.handleUserCreatedEvent(event))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void userCreatedHandler_recoverWithNullRepository_throwsNullPointerException() {
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), null);
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        var exception = new DataAccessResourceFailureException("DB error");
        assertThatThrownBy(() -> handler.recover(exception, event))
                .isInstanceOf(NullPointerException.class);
    }
}
