package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.CreateCourseProposalCommandHandler;
import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.common.event.FailedIntegrationEventRecord;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests the behavior when the {@code recover()} method itself fails because the
 * {@link FailedIntegrationEventRepository#save} call throws.
 * <p>
 * This is a critical edge case: if the DB is completely unavailable, both the original
 * handler AND the recovery method will fail. The recover() method does NOT catch
 * exceptions from the repository save — it lets them propagate. This is by design:
 * the {@code AsyncUncaughtExceptionHandler} will log the final failure, and the event
 * is effectively lost (acceptable for this system's requirements).
 * <p>
 * These tests document that contract and ensure no silent swallowing occurs.
 */
class RecoverMethodRepositoryFailureTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void sendCourseToApproveHandler_recover_whenRepositoryFails_exceptionPropagates() {
        // given
        var repo = mock(FailedIntegrationEventRepository.class);
        when(repo.save(any(FailedIntegrationEventRecord.class)))
                .thenThrow(new DataIntegrityViolationException("constraint violation: payload too large"));
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var originalException = new DataAccessResourceFailureException("original DB failure");

        // when/then — exception from repository propagates (not swallowed)
        assertThatThrownBy(() -> handler.recover(originalException, event))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("constraint violation");
    }

    @Test
    void courseApprovedHandler_recover_whenRepositoryFails_exceptionPropagates() {
        // given
        var repo = mock(FailedIntegrationEventRepository.class);
        when(repo.save(any(FailedIntegrationEventRecord.class)))
                .thenThrow(new DataAccessResourceFailureException("DB still down"));
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        var originalException = new DataAccessResourceFailureException("original failure");

        // when/then
        assertThatThrownBy(() -> handler.recover(originalException, event))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("DB still down");
    }

    @Test
    void studentEnrolledHandler_recover_whenRepositoryFails_exceptionPropagates() {
        // given
        var repo = mock(FailedIntegrationEventRepository.class);
        when(repo.save(any(FailedIntegrationEventRecord.class)))
                .thenThrow(new DataAccessResourceFailureException("connection refused"));
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        var originalException = new DataAccessResourceFailureException("original");

        // when/then
        assertThatThrownBy(() -> handler.recover(originalException, event))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("connection refused");
    }

    @Test
    void courseRatingHandler_recover_whenRepositoryFails_exceptionPropagates() {
        // given
        var repo = mock(FailedIntegrationEventRepository.class);
        when(repo.save(any(FailedIntegrationEventRecord.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        var originalException = new DataAccessResourceFailureException("timeout");

        // when/then
        assertThatThrownBy(() -> handler.recover(originalException, event))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("duplicate key");
    }

    @Test
    void userCreatedHandler_recover_whenRepositoryFails_exceptionPropagates() {
        // given
        var repo = mock(FailedIntegrationEventRepository.class);
        when(repo.save(any(FailedIntegrationEventRecord.class)))
                .thenThrow(new DataAccessResourceFailureException("disk full"));
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        var originalException = new DataAccessResourceFailureException("connection lost");

        // when/then
        assertThatThrownBy(() -> handler.recover(originalException, event))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("disk full");
    }

    @Test
    void sendCourseToApproveHandler_recover_whenRepositoryThrowsRuntimeException_propagates() {
        // given — unexpected RuntimeException from repository (e.g., NPE in save)
        var repo = mock(FailedIntegrationEventRepository.class);
        when(repo.save(any(FailedIntegrationEventRecord.class)))
                .thenThrow(new NullPointerException("unexpected null in entity manager"));
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var originalException = new DataAccessResourceFailureException("original");

        // when/then
        assertThatThrownBy(() -> handler.recover(originalException, event))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("unexpected null");
    }

    @Test
    void sendCourseToApproveHandler_recover_repositoryCalledExactlyOnce_evenOnFailure() {
        // given
        var repo = mock(FailedIntegrationEventRepository.class);
        when(repo.save(any(FailedIntegrationEventRecord.class)))
                .thenThrow(new DataIntegrityViolationException("constraint"));
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);

        // when
        try {
            handler.recover(new DataAccessResourceFailureException("error"), event);
        } catch (DataIntegrityViolationException ignored) {}

        // then — no retry of the save within recover()
        verify(repo, times(1)).save(any(FailedIntegrationEventRecord.class));
    }
}
