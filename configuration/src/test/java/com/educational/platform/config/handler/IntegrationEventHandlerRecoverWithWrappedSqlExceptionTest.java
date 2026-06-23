package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.CreateCourseProposalCommandHandler;
import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.common.event.FailedIntegrationEventRecord;
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
import org.mockito.ArgumentCaptor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifies that all handler {@code recover()} methods correctly persist dead-letter
 * records when the exception wraps a low-level SQL cause — a common production scenario
 * where Spring translates JDBC/SQL exceptions into {@code DataAccessException} subtypes.
 * <p>
 * These tests use {@link CannotAcquireLockException}, {@link PessimisticLockingFailureException},
 * and {@link QueryTimeoutException} — real-world exceptions that production handlers will
 * encounter but that are not exercised by the per-handler unit tests (which focus on
 * {@code DataAccessResourceFailureException} and {@code ObjectOptimisticLockingFailureException}).
 */
class IntegrationEventHandlerRecoverWithWrappedSqlExceptionTest {

    @Test
    void sendCourseToApproveHandler_recover_withCannotAcquireLockException_persistsCorrectClassName() throws Exception {
        // given — CannotAcquireLockException wraps a SQL deadlock cause
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(UUID.randomUUID());
        var sqlCause = new SQLException("Lock wait timeout exceeded");
        var exception = new CannotAcquireLockException("Could not acquire lock", sqlCause);

        // when
        handler.recover(exception, event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionClassName"))
                .isEqualTo(CannotAcquireLockException.class.getName());
        assertThat(getField(captor.getValue(), "exceptionMessage"))
                .isEqualTo("Could not acquire lock");
    }

    @Test
    void courseApprovedHandler_recover_withPessimisticLockingFailureException_persistsCorrectClassName() throws Exception {
        // given — PessimisticLockingFailureException represents a lock contention scenario
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        var event = new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID());
        var exception = new PessimisticLockingFailureException("Lock contention detected");

        // when
        handler.recover(exception, event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionClassName"))
                .isEqualTo(PessimisticLockingFailureException.class.getName());
        assertThat(getField(captor.getValue(), "exceptionMessage"))
                .isEqualTo("Lock contention detected");
    }

    @Test
    void studentEnrolledHandler_recover_withQueryTimeoutException_persistsCorrectClassName() throws Exception {
        // given — QueryTimeoutException wraps a slow-query SQL timeout
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(UUID.randomUUID(), "student1");
        var exception = new QueryTimeoutException("Query timed out after 30s", new SQLException("statement timeout"));

        // when
        handler.recover(exception, event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionClassName"))
                .isEqualTo(QueryTimeoutException.class.getName());
        assertThat(getField(captor.getValue(), "exceptionMessage"))
                .isEqualTo("Query timed out after 30s");
    }

    @Test
    void courseRatingHandler_recover_withCannotAcquireLockException_persistsEventPayloadWithRating() throws Exception {
        // given
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var courseId = UUID.randomUUID();
        var event = new CourseRatingRecalculatedIntegrationEvent(courseId, 4.75);
        var exception = new CannotAcquireLockException("row locked", new SQLException("lock timeout"));

        // when
        handler.recover(exception, event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "eventPayload"))
                .contains(courseId.toString())
                .contains("4.75");
    }

    @Test
    void userCreatedHandler_recover_withDeadlockLoserException_persistsEventPayloadWithUsernameAndEmail() throws Exception {
        // given
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent("john.doe", "john@example.com");
        var exception = new PessimisticLockingFailureException("pessimistic lock contention");

        // when
        handler.recover(exception, event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "eventPayload"))
                .contains("john.doe")
                .contains("john@example.com");
    }

    @Test
    void allExceptionSubtypes_areDataAccessExceptions() {
        // guard: verify that these production exceptions are caught by the recover(DataAccessException) method
        assertThat(new CannotAcquireLockException("x", null))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(new PessimisticLockingFailureException("x"))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(new QueryTimeoutException("x", new SQLException("y")))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void sendCourseToApproveHandler_recover_wrappedException_topLevelClassNameStored_notCauseClassName() throws Exception {
        // given — verify that e.getClass().getName() stores the Spring wrapper, not the SQL cause
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(UUID.randomUUID());
        var sqlCause = new SQLException("ORA-00060: deadlock detected");
        var exception = new DataAccessResourceFailureException("JDBC resource failure", sqlCause);

        // when
        handler.recover(exception, event);

        // then — the persisted class name is the Spring exception, not the SQL cause
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionClassName"))
                .isEqualTo(DataAccessResourceFailureException.class.getName())
                .isNotEqualTo(SQLException.class.getName());
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
