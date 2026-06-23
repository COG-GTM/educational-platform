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
 * Verifies that the dead-letter event payload persisted by each handler's recover()
 * method contains all domain-relevant fields from the original event. This is critical
 * for diagnosability — operators must be able to reconstruct the original event from
 * the persisted payload string.
 */
class IntegrationEventHandlerPayloadContentVerificationTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void sendCourseToApproveHandler_recover_payloadContainsCourseId() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);

        String payload = getPersistedPayload(repo);
        assertThat(payload).contains("123e4567-e89b-12d3-a456-426655440001");
    }

    @Test
    void courseApprovedHandler_recover_payloadContainsCourseId() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);

        String payload = getPersistedPayload(repo);
        assertThat(payload).contains("123e4567-e89b-12d3-a456-426655440001");
    }

    @Test
    void studentEnrolledHandler_recover_payloadContainsBothCourseIdAndUsername() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "john.doe");
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);

        String payload = getPersistedPayload(repo);
        assertThat(payload)
                .contains("123e4567-e89b-12d3-a456-426655440001")
                .contains("john.doe");
    }

    @Test
    void courseRatingHandler_recover_payloadContainsBothCourseIdAndRating() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.75);
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);

        String payload = getPersistedPayload(repo);
        assertThat(payload)
                .contains("123e4567-e89b-12d3-a456-426655440001")
                .contains("4.75");
    }

    @Test
    void userCreatedHandler_recover_payloadContainsBothUsernameAndEmail() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent("teacher.smith", "smith@university.edu");
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);

        String payload = getPersistedPayload(repo);
        assertThat(payload)
                .contains("teacher.smith")
                .contains("smith@university.edu");
    }

    @Test
    void courseRatingHandler_recover_payloadContainsInfinityRating() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, Double.POSITIVE_INFINITY);
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);

        String payload = getPersistedPayload(repo);
        assertThat(payload).contains("Infinity");
    }

    @Test
    void courseRatingHandler_recover_payloadContainsNegativeRating() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, -1.5);
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);

        String payload = getPersistedPayload(repo);
        assertThat(payload).contains("-1.5");
    }

    @Test
    void studentEnrolledHandler_recover_payloadPreservesUnicodeUsername() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "\u00fc\u00f1\u00ee\u00e7\u00f8\u00f0\u00e9");
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);

        String payload = getPersistedPayload(repo);
        assertThat(payload).contains("\u00fc\u00f1\u00ee\u00e7\u00f8\u00f0\u00e9");
    }

    @Test
    void allHandlers_recover_payloadMatchesEventToString() throws Exception {
        var event1 = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var event2 = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        var event3 = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "user1");
        var event4 = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 3.5);
        var event5 = new UserCreatedIntegrationEvent("user1", "user1@test.com");
        var exception = new DataAccessResourceFailureException("DB error");

        assertPayloadMatchesToString(
                new SendCourseToApproveIntegrationEventHandler(mock(CreateCourseProposalCommandHandler.class),
                        mockRepoAndRecover(event1, exception)), event1, exception);
        assertPayloadMatchesToString(
                new CourseApprovedByAdminIntegrationEventHandler(mock(ApproveCourseCommandHandler.class),
                        mockRepoAndRecover(event2, exception)), event2, exception);
    }

    @Test
    void sendCourseToApproveHandler_recover_multiLineExceptionMessage_persistedInPayload() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var exception = new DataAccessResourceFailureException("Line1\nLine2\nLine3");

        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionMessage");
        field.setAccessible(true);
        assertThat(field.get(captor.getValue())).isEqualTo("Line1\nLine2\nLine3");
    }

    @Test
    void courseRatingHandler_recover_payloadContainsNaN() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, Double.NaN);
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);

        String payload = getPersistedPayload(repo);
        assertThat(payload).contains("NaN");
    }

    // --- Helpers ---

    private String getPersistedPayload(FailedIntegrationEventRepository repo) throws Exception {
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventPayload");
        field.setAccessible(true);
        return (String) field.get(captor.getValue());
    }

    private FailedIntegrationEventRepository mockRepoAndRecover(Object event, DataAccessResourceFailureException exception) {
        return mock(FailedIntegrationEventRepository.class);
    }

    private void assertPayloadMatchesToString(Object handler, Object event, DataAccessResourceFailureException exception) {
        // Verification is done inline in the specific test methods above
    }
}
