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
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Verifies that every handler's {@code recover()} method produces a
 * {@link FailedIntegrationEventRecord} containing all diagnostic information
 * needed for operators to identify, diagnose, and manually replay the failed event.
 * <p>
 * This is a cross-cutting end-to-end verification: for each handler type, we invoke
 * recover() with a realistic exception and verify the resulting dead-letter record
 * contains:
 * <ul>
 *   <li>The full event class name (for routing/filtering)</li>
 *   <li>The event toString() payload (for diagnosis)</li>
 *   <li>The exception message and class name (for root cause analysis)</li>
 *   <li>The retry count (matches MAX_ATTEMPTS for correlation with metrics)</li>
 *   <li>A FAILED status and a createdAt timestamp</li>
 * </ul>
 */
class DeadLetterRecordDiagnosticCompletenessTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static final String EXCEPTION_MSG = "Connection timed out after 30000ms";

    @Test
    void sendCourseToApproveHandler_recover_producesCompleteDiagnosticRecord() throws Exception {
        // given
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var exception = new DataAccessResourceFailureException(EXCEPTION_MSG);

        // when
        handler.recover(exception, event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertDiagnosticCompleteness(captor.getValue(), event, exception);
    }

    @Test
    void courseApprovedByAdminHandler_recover_producesCompleteDiagnosticRecord() throws Exception {
        // given
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        var exception = new DataAccessResourceFailureException(EXCEPTION_MSG);

        // when
        handler.recover(exception, event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertDiagnosticCompleteness(captor.getValue(), event, exception);
    }

    @Test
    void studentEnrolledToCourseHandler_recover_producesCompleteDiagnosticRecord() throws Exception {
        // given
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        var exception = new DataAccessResourceFailureException(EXCEPTION_MSG);

        // when
        handler.recover(exception, event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertDiagnosticCompleteness(captor.getValue(), event, exception);
    }

    @Test
    void courseRatingRecalculatedHandler_recover_producesCompleteDiagnosticRecord() throws Exception {
        // given
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        var exception = new DataAccessResourceFailureException(EXCEPTION_MSG);

        // when
        handler.recover(exception, event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertDiagnosticCompleteness(captor.getValue(), event, exception);
    }

    @Test
    void userCreatedHandler_recover_producesCompleteDiagnosticRecord() throws Exception {
        // given
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        var exception = new DataAccessResourceFailureException(EXCEPTION_MSG);

        // when
        handler.recover(exception, event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertDiagnosticCompleteness(captor.getValue(), event, exception);
    }

    @Test
    void allHandlers_recover_eventPayloadContainsCourseId() throws Exception {
        // Verify courseId is included in the dead-letter payload for course-related events
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        handler.recover(new DataAccessResourceFailureException("error"), event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "eventPayload"))
                .contains(COURSE_ID.toString());
    }

    @Test
    void userCreatedHandler_recover_eventPayloadContainsUsername() throws Exception {
        // Verify username is included in the dead-letter payload for user events
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent("important-teacher", "teacher@test.com");
        handler.recover(new DataAccessResourceFailureException("error"), event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "eventPayload"))
                .contains("important-teacher");
    }

    @Test
    void courseRatingHandler_recover_eventPayloadContainsRating() throws Exception {
        // Verify rating value is included in the dead-letter payload
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 3.75);
        handler.recover(new DataAccessResourceFailureException("error"), event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "eventPayload"))
                .contains("3.75");
    }

    @Test
    void studentEnrolledHandler_recover_eventPayloadContainsUsername() throws Exception {
        // Verify student username is included in the dead-letter payload
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "enrolled-student");
        handler.recover(new DataAccessResourceFailureException("error"), event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "eventPayload"))
                .contains("enrolled-student");
    }

    @Test
    void courseRatingHandler_recover_withEdgeCaseValues_diagnosticRecordStillComplete() throws Exception {
        // given — NaN rating + null courseId: extreme edge case where both fields are edge values
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(null, Double.NaN);
        var exception = new DataAccessResourceFailureException(EXCEPTION_MSG);

        // when
        handler.recover(exception, event);

        // then — even with edge-case values, all diagnostic fields must be populated
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertDiagnosticCompleteness(captor.getValue(), event, exception);
    }

    @Test
    void studentEnrolledHandler_recover_withBothNullFields_diagnosticRecordStillComplete() throws Exception {
        // given — both courseId and username are null
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(null, null);
        var exception = new DataAccessResourceFailureException(EXCEPTION_MSG);

        // when
        handler.recover(exception, event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertDiagnosticCompleteness(captor.getValue(), event, exception);
    }

    @Test
    void userCreatedHandler_recover_withBothNullFields_diagnosticRecordStillComplete() throws Exception {
        // given — both username and email are null
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent(null, null);
        var exception = new DataAccessResourceFailureException(EXCEPTION_MSG);

        // when
        handler.recover(exception, event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertDiagnosticCompleteness(captor.getValue(), event, exception);
    }

    private void assertDiagnosticCompleteness(FailedIntegrationEventRecord record,
                                               Object event,
                                               DataAccessResourceFailureException exception) throws Exception {
        // Event class name — needed for filtering/routing dead letters
        assertThat((String) getField(record, "eventClassName"))
                .as("eventClassName should be the FQCN of the event")
                .isEqualTo(event.getClass().getName())
                .startsWith("com.educational.platform.")
                .endsWith("IntegrationEvent");

        // Event payload — needed for diagnosis/replay
        assertThat((String) getField(record, "eventPayload"))
                .as("eventPayload should be the event's toString()")
                .isEqualTo(event.toString())
                .isNotBlank();

        // Exception message — root cause hint
        assertThat((String) getField(record, "exceptionMessage"))
                .as("exceptionMessage should match the exception")
                .isEqualTo(exception.getMessage());

        // Exception class name — needed for categorization
        assertThat((String) getField(record, "exceptionClassName"))
                .as("exceptionClassName should be the FQCN of the exception")
                .isEqualTo(exception.getClass().getName());

        // Retry count — matches MAX_ATTEMPTS for metric correlation
        assertThat((int) getField(record, "retryCount"))
                .as("retryCount should equal MAX_ATTEMPTS (3)")
                .isEqualTo(3);

        // Status — should be FAILED
        assertThat((FailedIntegrationEventRecord.Status) getField(record, "status"))
                .as("status should be FAILED")
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);

        // Timestamp — should be recent
        Instant createdAt = (Instant) getField(record, "createdAt");
        assertThat(createdAt)
                .as("createdAt should be set to a recent timestamp")
                .isNotNull()
                .isBefore(Instant.now().plusSeconds(1))
                .isAfter(Instant.now().minusSeconds(5));
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
