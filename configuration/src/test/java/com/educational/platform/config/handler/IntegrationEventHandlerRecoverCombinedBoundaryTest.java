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
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests recover() with combined boundary conditions: deeply nested cause chain AND
 * long exception message at the same time, and recover() idempotency with the exact
 * same event reference.
 */
class IntegrationEventHandlerRecoverCombinedBoundaryTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    // --- Combined: deeply nested cause + long message ---

    @Test
    void sendCourseToApproveHandler_recover_deeplyNestedCauseWithLongMessage_persistsBoth() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        String longMessage = "X".repeat(2000);
        var exception = createDeeplyNestedExceptionWithLongMessage(longMessage);

        handler.recover(exception, event);

        assertPersistedRecord(repo, longMessage, DataAccessResourceFailureException.class.getName());
    }

    @Test
    void courseApprovedHandler_recover_deeplyNestedCauseWithLongMessage_persistsBoth() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        String longMessage = "Y".repeat(2000);
        var exception = createDeeplyNestedExceptionWithLongMessage(longMessage);

        handler.recover(exception, event);

        assertPersistedRecord(repo, longMessage, DataAccessResourceFailureException.class.getName());
    }

    @Test
    void studentEnrolledHandler_recover_deeplyNestedCauseWithLongMessage_persistsBoth() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        String longMessage = "Z".repeat(2000);
        var exception = createDeeplyNestedExceptionWithLongMessage(longMessage);

        handler.recover(exception, event);

        assertPersistedRecord(repo, longMessage, DataAccessResourceFailureException.class.getName());
    }

    @Test
    void courseRatingHandler_recover_deeplyNestedCauseWithLongMessage_persistsBoth() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        String longMessage = "W".repeat(2000);
        var exception = createDeeplyNestedExceptionWithLongMessage(longMessage);

        handler.recover(exception, event);

        assertPersistedRecord(repo, longMessage, DataAccessResourceFailureException.class.getName());
    }

    @Test
    void userCreatedHandler_recover_deeplyNestedCauseWithLongMessage_persistsBoth() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        String longMessage = "V".repeat(2000);
        var exception = createDeeplyNestedExceptionWithLongMessage(longMessage);

        handler.recover(exception, event);

        assertPersistedRecord(repo, longMessage, DataAccessResourceFailureException.class.getName());
    }

    // --- Idempotency: same event reference passed to recover() twice ---

    @Test
    void sendCourseToApproveHandler_recover_sameEventReferenceTwice_producesTwoIndependentRecords() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);
        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0)).isNotSameAs(captor.getAllValues().get(1));
    }

    @Test
    void courseApprovedHandler_recover_sameEventReferenceTwice_producesTwoIndependentRecords() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);
        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0)).isNotSameAs(captor.getAllValues().get(1));
    }

    @Test
    void studentEnrolledHandler_recover_sameEventReferenceTwice_producesTwoIndependentRecords() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);
        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0)).isNotSameAs(captor.getAllValues().get(1));
    }

    @Test
    void courseRatingHandler_recover_sameEventReferenceTwice_producesTwoIndependentRecords() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);
        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0)).isNotSameAs(captor.getAllValues().get(1));
    }

    @Test
    void userCreatedHandler_recover_sameEventReferenceTwice_producesTwoIndependentRecords() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);
        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0)).isNotSameAs(captor.getAllValues().get(1));
    }

    // --- Helpers ---

    private DataAccessResourceFailureException createDeeplyNestedExceptionWithLongMessage(String topMessage) {
        var root = new IOException("root: connection reset by peer");
        var mid1 = new SQLException("mid: prepared statement failed", root);
        var mid2 = new RuntimeException("mid: transaction rolled back", mid1);
        return new DataAccessResourceFailureException(topMessage, mid2);
    }

    private void assertPersistedRecord(FailedIntegrationEventRepository repo,
                                        String expectedMessage,
                                        String expectedExceptionClassName) throws Exception {
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        var record = captor.getValue();

        Field messageField = FailedIntegrationEventRecord.class.getDeclaredField("exceptionMessage");
        messageField.setAccessible(true);
        assertThat(messageField.get(record)).isEqualTo(expectedMessage);

        Field classNameField = FailedIntegrationEventRecord.class.getDeclaredField("exceptionClassName");
        classNameField.setAccessible(true);
        assertThat(classNameField.get(record)).isEqualTo(expectedExceptionClassName);

        Field createdAtField = FailedIntegrationEventRecord.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        assertThat((Instant) createdAtField.get(record)).isNotNull();
    }
}
