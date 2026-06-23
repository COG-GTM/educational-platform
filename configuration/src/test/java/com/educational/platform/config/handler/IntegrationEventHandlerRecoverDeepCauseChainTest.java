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
import org.springframework.dao.DataAccessResourceFailureException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Verifies that when a handler's recover method receives an exception with a deep
 * cause chain (3+ levels), only the top-level exception's message and class name
 * are persisted in the dead-letter record. The cause chain should NOT leak into
 * the stored fields, keeping the record compact and predictable.
 */
class IntegrationEventHandlerRecoverDeepCauseChainTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void sendCourseToApprove_recover_deepCauseChain_onlyTopLevelMessagePersisted() throws Exception {
        var deepCause = new ConnectException("Connection refused: localhost:5432");
        var midCause = new IOException("Failed to open JDBC connection", deepCause);
        var topLevel = new DataAccessResourceFailureException("Could not acquire connection", midCause);

        var record = recoverSendCourseToApprove(topLevel);

        assertThat((String) getField(record, "exceptionMessage"))
                .isEqualTo("Could not acquire connection")
                .doesNotContain("Connection refused")
                .doesNotContain("Failed to open JDBC connection");
        assertThat((String) getField(record, "exceptionClassName"))
                .isEqualTo(DataAccessResourceFailureException.class.getName())
                .doesNotContain("IOException")
                .doesNotContain("ConnectException");
    }

    @Test
    void courseApprovedByAdmin_recover_deepCauseChain_onlyTopLevelMessagePersisted() throws Exception {
        var deepCause = new ConnectException("Connection refused");
        var midCause = new RuntimeException("Pool exhausted", deepCause);
        var topLevel = new DataAccessResourceFailureException("Cannot acquire connection from pool", midCause);

        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(mock(ApproveCourseCommandHandler.class), repo);
        handler.recover(topLevel, new CourseApprovedByAdminIntegrationEvent(COURSE_ID));

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "exceptionMessage"))
                .isEqualTo("Cannot acquire connection from pool");
        assertThat((String) getField(captor.getValue(), "exceptionClassName"))
                .isEqualTo(DataAccessResourceFailureException.class.getName());
    }

    @Test
    void studentEnrolled_recover_deepCauseChain_onlyTopLevelMessagePersisted() throws Exception {
        var deepCause = new java.sql.SQLException("Lock wait timeout exceeded");
        var midCause = new RuntimeException("Statement execution failed", deepCause);
        var topLevel = new DataAccessResourceFailureException("Could not execute statement", midCause);

        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        handler.recover(topLevel, new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1"));

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "exceptionMessage"))
                .isEqualTo("Could not execute statement")
                .doesNotContain("Lock wait timeout");
    }

    @Test
    void courseRating_recover_deepCauseChain_onlyTopLevelMessagePersisted() throws Exception {
        var deepCause = new ArithmeticException("Division by zero");
        var midCause = new IllegalStateException("Rating calculation failed", deepCause);
        var topLevel = new DataAccessResourceFailureException("Could not update course rating", midCause);

        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        handler.recover(topLevel, new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5));

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "exceptionMessage"))
                .isEqualTo("Could not update course rating")
                .doesNotContain("Division by zero");
    }

    @Test
    void userCreated_recover_deepCauseChain_onlyTopLevelMessagePersisted() throws Exception {
        var deepCause = new java.sql.SQLIntegrityConstraintViolationException("Duplicate entry 'teacher1'");
        var midCause = new RuntimeException("Constraint violation on INSERT", deepCause);
        var topLevel = new DataAccessResourceFailureException("Could not execute INSERT", midCause);

        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        handler.recover(topLevel, new UserCreatedIntegrationEvent("teacher1", "teacher1@edu.com"));

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "exceptionMessage"))
                .isEqualTo("Could not execute INSERT")
                .doesNotContain("Duplicate entry");
    }

    @Test
    void recover_fourLevelCauseChain_onlyTopLevelPersisted() throws Exception {
        var level4 = new NullPointerException("entity was null");
        var level3 = new IllegalArgumentException("Invalid entity state", level4);
        var level2 = new RuntimeException("Transaction rollback", level3);
        var level1 = new DataAccessResourceFailureException("Rollback on commit", level2);

        var record = recoverSendCourseToApprove(level1);

        assertThat((String) getField(record, "exceptionMessage"))
                .isEqualTo("Rollback on commit")
                .doesNotContain("entity was null")
                .doesNotContain("Invalid entity state")
                .doesNotContain("Transaction rollback");
    }

    @Test
    void recover_selfReferencingCause_onlyTopLevelMessagePersisted() throws Exception {
        var topLevel = new DataAccessResourceFailureException("Self-referencing error");

        var record = recoverSendCourseToApprove(topLevel);

        assertThat((String) getField(record, "exceptionMessage"))
                .isEqualTo("Self-referencing error");
        assertThat(topLevel.getCause()).isNull();
    }

    private FailedIntegrationEventRecord recoverSendCourseToApprove(
            DataAccessResourceFailureException exception) throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        handler.recover(exception, new SendCourseToApproveIntegrationEvent(COURSE_ID));

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        return captor.getValue();
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
