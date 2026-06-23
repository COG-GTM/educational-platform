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
import static org.mockito.Mockito.*;

/**
 * Verifies that handler recover methods correctly persist exception messages
 * containing special characters commonly found in real-world database errors:
 * SQL fragments, quotes, backslashes, newlines, and null bytes.
 */
class IntegrationEventHandlerRecoverSpecialCharactersTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void recover_withSqlFragmentInExceptionMessage_persistsCorrectly() throws Exception {
        String sqlMessage = "ERROR: duplicate key value violates unique constraint \"pk_courses\" "
                + "Detail: Key (id)=(123e4567-e89b-12d3-a456-426655440001) already exists.";
        var record = recoverSendCourseToApprove(sqlMessage);
        assertThat((String) getField(record, "exceptionMessage")).isEqualTo(sqlMessage);
    }

    @Test
    void recover_withSingleQuotesInExceptionMessage_persistsCorrectly() throws Exception {
        String message = "Column 'event_class_name' cannot be null";
        var record = recoverSendCourseToApprove(message);
        assertThat((String) getField(record, "exceptionMessage")).isEqualTo(message);
    }

    @Test
    void recover_withBackslashesInExceptionMessage_persistsCorrectly() throws Exception {
        String message = "File not found: C:\\Users\\admin\\data\\export.csv";
        var record = recoverSendCourseToApprove(message);
        assertThat((String) getField(record, "exceptionMessage")).isEqualTo(message);
    }

    @Test
    void recover_withNewlinesInExceptionMessage_persistsCorrectly() throws Exception {
        String message = "Connection failed\n\tat com.example.db.Pool.acquire(Pool.java:42)\n\tat com.example.service.save(Service.java:10)";
        var record = recoverSendCourseToApprove(message);
        assertThat((String) getField(record, "exceptionMessage")).isEqualTo(message);
    }

    @Test
    void recover_withTabsAndCarriageReturnsInExceptionMessage_persistsCorrectly() throws Exception {
        String message = "Lock timeout\r\n\tResource: TABLE\r\n\tOwner: TX-001";
        var record = recoverSendCourseToApprove(message);
        assertThat((String) getField(record, "exceptionMessage")).isEqualTo(message);
    }

    @Test
    void courseApproved_recover_withSqlFragment_persistsCorrectly() throws Exception {
        String sqlMessage = "ERROR: deadlock detected; Details: Process 42 waits for ShareLock on transaction 100";
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(mock(ApproveCourseCommandHandler.class), repo);
        handler.recover(new DataAccessResourceFailureException(sqlMessage), new CourseApprovedByAdminIntegrationEvent(COURSE_ID));

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "exceptionMessage")).isEqualTo(sqlMessage);
    }

    @Test
    void studentEnrolled_recover_withSpecialChars_persistsCorrectly() throws Exception {
        String message = "Batch update returned unexpected row count from update [0]; actual row count: 0; expected: 1";
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        handler.recover(new DataAccessResourceFailureException(message),
                new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1"));

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "exceptionMessage")).isEqualTo(message);
    }

    @Test
    void courseRating_recover_withSpecialChars_persistsCorrectly() throws Exception {
        String message = "could not execute statement; SQL [INSERT INTO course_ratings (course_id, rating) VALUES (?, ?)]; constraint [\"UK_RATING\"]";
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        handler.recover(new DataAccessResourceFailureException(message),
                new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5));

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "exceptionMessage")).isEqualTo(message);
    }

    @Test
    void userCreated_recover_withSpecialChars_persistsCorrectly() throws Exception {
        String message = "Unique index or primary key violation: \"PUBLIC.UK_USERS_USERNAME_INDEX_1 ON PUBLIC.USERS(USERNAME) VALUES ( /* 1 */ 'teacher1' )\"";
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        handler.recover(new DataAccessResourceFailureException(message),
                new UserCreatedIntegrationEvent("teacher1", "teacher1@edu.com"));

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "exceptionMessage")).isEqualTo(message);
    }

    private FailedIntegrationEventRecord recoverSendCourseToApprove(String exceptionMessage) throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        handler.recover(new DataAccessResourceFailureException(exceptionMessage),
                new SendCourseToApproveIntegrationEvent(COURSE_ID));

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
