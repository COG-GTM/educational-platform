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

import java.lang.reflect.Field;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests that all handlers correctly persist multi-line and stack-trace-style
 * exception messages in the dead-letter record. This is important because
 * DataAccessExceptions often include multi-line details (e.g., SQL error context,
 * nested cause summaries) that must be preserved for operator diagnosis.
 */
class RecoverMethodMultiLineExceptionTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void sendCourseToApproveHandler_recover_multiLineException_preserved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        String multiLine = "Connection refused\n  host=db.example.com\n  port=5432\n  timeout=30s";
        var exception = new DataAccessResourceFailureException(multiLine);

        handler.recover(exception, event);

        assertPersistedMessage(repo, multiLine);
    }

    @Test
    void courseApprovedHandler_recover_multiLineException_preserved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        String multiLine = "Deadlock detected\n  Transaction A holds lock on row 1\n  Transaction B holds lock on row 2";
        var exception = new DataAccessResourceFailureException(multiLine);

        handler.recover(exception, event);

        assertPersistedMessage(repo, multiLine);
    }

    @Test
    void studentEnrolledHandler_recover_multiLineException_preserved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        String multiLine = "Constraint violation\n  Table: course_students\n  Column: course_id\n  Value: null";
        var exception = new DataAccessResourceFailureException(multiLine);

        handler.recover(exception, event);

        assertPersistedMessage(repo, multiLine);
    }

    @Test
    void courseRatingHandler_recover_multiLineException_preserved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        String multiLine = "PreparedStatement failed\n  SQL: UPDATE courses SET rating = ?\n  Params: [4.5]";
        var exception = new DataAccessResourceFailureException(multiLine);

        handler.recover(exception, event);

        assertPersistedMessage(repo, multiLine);
    }

    @Test
    void userCreatedHandler_recover_multiLineException_preserved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        String multiLine = "Unique constraint violated\n  Index: idx_teachers_username\n  Key: teacher1";
        var exception = new DataAccessResourceFailureException(multiLine);

        handler.recover(exception, event);

        assertPersistedMessage(repo, multiLine);
    }

    @Test
    void allHandlers_recover_windowsLineEndings_preserved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        String windowsLines = "Error line 1\r\nError line 2\r\nError line 3";
        var exception = new DataAccessResourceFailureException(windowsLines);

        handler.recover(exception, event);

        assertPersistedMessage(repo, windowsLines);
    }

    @Test
    void allHandlers_recover_tabSeparatedMessage_preserved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        String tabbedMessage = "Field\tExpected\tActual\ncourse_id\t123\tnull";
        var exception = new DataAccessResourceFailureException(tabbedMessage);

        handler.recover(exception, event);

        assertPersistedMessage(repo, tabbedMessage);
    }

    // --- Helpers ---

    private void assertPersistedMessage(FailedIntegrationEventRepository repo, String expectedMessage) throws Exception {
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionMessage");
        field.setAccessible(true);
        assertThat(field.get(captor.getValue())).isEqualTo(expectedMessage);
    }
}
