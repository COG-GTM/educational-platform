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
import org.mockito.ArgumentCaptor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests that {@code recover()} correctly handles real-world {@code DataAccessException}
 * subtypes that commonly occur in production but are not the "default" test exceptions.
 * <p>
 * The handlers' {@code @Recover(DataAccessException)} method accepts any
 * {@code DataAccessException} subclass. These tests verify that less-common but
 * production-relevant subtypes are correctly persisted as dead-letter records:
 * <ul>
 *   <li>{@link CannotAcquireLockException} — row/table lock timeout</li>
 *   <li>{@link DeadlockLoserDataAccessException} — chosen as deadlock victim</li>
 *   <li>{@link QueryTimeoutException} — SQL query timeout</li>
 *   <li>{@link TransientDataAccessResourceException} — transient connection issue</li>
 * </ul>
 */
class IntegrationEventHandlerRecoverRealWorldExceptionsTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    // --- CannotAcquireLockException ---

    @Test
    void sendCourseToApproveHandler_recover_cannotAcquireLock_persistsCorrectExceptionClass() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var exception = new CannotAcquireLockException("Lock wait timeout exceeded");

        handler.recover(exception, event);

        assertPersistedExceptionClassName(repo, CannotAcquireLockException.class.getName());
    }

    @Test
    void courseApprovedHandler_recover_cannotAcquireLock_persistsCorrectExceptionClass() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        var exception = new CannotAcquireLockException("Lock wait timeout exceeded");

        handler.recover(exception, event);

        assertPersistedExceptionClassName(repo, CannotAcquireLockException.class.getName());
    }

    // --- DeadlockLoserDataAccessException ---

    @Test
    void studentEnrolledHandler_recover_deadlockLoser_persistsCorrectFields() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        var exception = new DeadlockLoserDataAccessException("Deadlock found when trying to get lock", null);

        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionClassName"))
                .isEqualTo(DeadlockLoserDataAccessException.class.getName());
        assertThat(getField(captor.getValue(), "exceptionMessage"))
                .isEqualTo("Deadlock found when trying to get lock");
    }

    @Test
    void courseRatingHandler_recover_deadlockLoser_persistsCorrectFields() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        var exception = new DeadlockLoserDataAccessException("Transaction was chosen as deadlock victim", null);

        handler.recover(exception, event);

        assertPersistedExceptionClassName(repo, DeadlockLoserDataAccessException.class.getName());
    }

    // --- QueryTimeoutException ---

    @Test
    void userCreatedHandler_recover_queryTimeout_persistsCorrectFields() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        var exception = new QueryTimeoutException("Query timed out after 30000ms");

        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionClassName"))
                .isEqualTo(QueryTimeoutException.class.getName());
        assertThat(getField(captor.getValue(), "exceptionMessage"))
                .isEqualTo("Query timed out after 30000ms");
    }

    // --- TransientDataAccessResourceFailureException ---

    @Test
    void sendCourseToApproveHandler_recover_transientFailure_persistsCorrectFields() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var exception = new TransientDataAccessResourceException("Connection pool exhausted");

        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionClassName"))
                .isEqualTo(TransientDataAccessResourceException.class.getName());
        assertThat(getField(captor.getValue(), "exceptionMessage"))
                .isEqualTo("Connection pool exhausted");
    }

    // --- Helpers ---

    private void assertPersistedExceptionClassName(FailedIntegrationEventRepository repo,
                                                    String expectedClassName) throws Exception {
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionClassName")).isEqualTo(expectedClassName);
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
