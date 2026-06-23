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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Verifies that calling {@code recover()} multiple times with the same event and
 * exception produces independent dead-letter records each time. This guards against
 * accidental deduplication logic that could silently discard retry exhaustion signals.
 * <p>
 * In production, if a handler's retry is exhausted concurrently for the same event
 * (e.g., duplicate message delivery), each exhaustion MUST produce its own dead-letter
 * record for complete audit trail.
 */
class IntegrationEventHandlerRecoverIdempotencyTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void sendCourseToApproveHandler_recoverCalledTwice_producesTwoRecords() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);
        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertRecordsHaveSameContent(captor.getAllValues().get(0), captor.getAllValues().get(1));
    }

    @Test
    void courseApprovedHandler_recoverCalledTwice_producesTwoRecords() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);
        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertRecordsHaveSameContent(captor.getAllValues().get(0), captor.getAllValues().get(1));
    }

    @Test
    void studentEnrolledHandler_recoverCalledTwice_producesTwoRecords() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);
        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertRecordsHaveSameContent(captor.getAllValues().get(0), captor.getAllValues().get(1));
    }

    @Test
    void courseRatingHandler_recoverCalledTwice_producesTwoRecords() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);
        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertRecordsHaveSameContent(captor.getAllValues().get(0), captor.getAllValues().get(1));
    }

    @Test
    void userCreatedHandler_recoverCalledTwice_producesTwoRecords() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);
        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertRecordsHaveSameContent(captor.getAllValues().get(0), captor.getAllValues().get(1));
    }

    @Test
    void sendCourseToApproveHandler_recoverWithDifferentExceptions_producesDifferentRecords() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var exception1 = new DataAccessResourceFailureException("Connection refused");
        var exception2 = new DataAccessResourceFailureException("Timeout expired");

        handler.recover(exception1, event);
        handler.recover(exception2, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(2)).save(captor.capture());
        assertThat(getField(captor.getAllValues().get(0), "exceptionMessage"))
                .isEqualTo("Connection refused");
        assertThat(getField(captor.getAllValues().get(1), "exceptionMessage"))
                .isEqualTo("Timeout expired");
    }

    private void assertRecordsHaveSameContent(FailedIntegrationEventRecord first,
                                              FailedIntegrationEventRecord second) throws Exception {
        assertThat(getField(first, "eventClassName")).isEqualTo(getField(second, "eventClassName"));
        assertThat(getField(first, "eventPayload")).isEqualTo(getField(second, "eventPayload"));
        assertThat(getField(first, "exceptionMessage")).isEqualTo(getField(second, "exceptionMessage"));
        assertThat(getField(first, "exceptionClassName")).isEqualTo(getField(second, "exceptionClassName"));
        assertThat((int) getField(first, "retryCount")).isEqualTo((int) getField(second, "retryCount"));
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
