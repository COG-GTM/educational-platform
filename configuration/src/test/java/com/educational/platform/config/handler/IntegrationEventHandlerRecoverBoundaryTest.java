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
 * Tests recover() boundary conditions across all handlers: deeply nested exception
 * cause chains, very long exception messages, and exception messages at column limits.
 */
class IntegrationEventHandlerRecoverBoundaryTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    // --- Deeply nested cause chains ---

    @Test
    void sendCourseToApproveHandler_recover_deeplyNestedCause_persistsTopLevelMessage() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var exception = createDeeplyNestedException("top-level DB failure");

        handler.recover(exception, event);

        assertPersistedExceptionMessage(repo, "top-level DB failure");
    }

    @Test
    void courseApprovedHandler_recover_deeplyNestedCause_persistsTopLevelMessage() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        var exception = createDeeplyNestedException("approval DB failure");

        handler.recover(exception, event);

        assertPersistedExceptionMessage(repo, "approval DB failure");
    }

    @Test
    void studentEnrolledHandler_recover_deeplyNestedCause_persistsTopLevelMessage() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        var exception = createDeeplyNestedException("enrollment DB failure");

        handler.recover(exception, event);

        assertPersistedExceptionMessage(repo, "enrollment DB failure");
    }

    @Test
    void courseRatingHandler_recover_deeplyNestedCause_persistsTopLevelMessage() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        var exception = createDeeplyNestedException("rating DB failure");

        handler.recover(exception, event);

        assertPersistedExceptionMessage(repo, "rating DB failure");
    }

    @Test
    void userCreatedHandler_recover_deeplyNestedCause_persistsTopLevelMessage() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        var exception = createDeeplyNestedException("user DB failure");

        handler.recover(exception, event);

        assertPersistedExceptionMessage(repo, "user DB failure");
    }

    // --- Very long exception messages (near 2000 char column limit) ---

    @Test
    void sendCourseToApproveHandler_recover_longExceptionMessage_persistsFullMessage() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        String longMessage = "X".repeat(2000);
        var exception = new DataAccessResourceFailureException(longMessage);

        handler.recover(exception, event);

        assertPersistedExceptionMessage(repo, longMessage);
    }

    @Test
    void courseApprovedHandler_recover_longExceptionMessage_persistsFullMessage() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        String longMessage = "Y".repeat(2000);
        var exception = new DataAccessResourceFailureException(longMessage);

        handler.recover(exception, event);

        assertPersistedExceptionMessage(repo, longMessage);
    }

    @Test
    void studentEnrolledHandler_recover_longExceptionMessage_persistsFullMessage() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        String longMessage = "Z".repeat(2000);
        var exception = new DataAccessResourceFailureException(longMessage);

        handler.recover(exception, event);

        assertPersistedExceptionMessage(repo, longMessage);
    }

    @Test
    void courseRatingHandler_recover_longExceptionMessage_persistsFullMessage() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        String longMessage = "W".repeat(2000);
        var exception = new DataAccessResourceFailureException(longMessage);

        handler.recover(exception, event);

        assertPersistedExceptionMessage(repo, longMessage);
    }

    @Test
    void userCreatedHandler_recover_longExceptionMessage_persistsFullMessage() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        String longMessage = "V".repeat(2000);
        var exception = new DataAccessResourceFailureException(longMessage);

        handler.recover(exception, event);

        assertPersistedExceptionMessage(repo, longMessage);
    }

    // --- Exception message exceeding column limit (accepted at Java level, fails at DB) ---

    @Test
    void allHandlers_recover_exceptionMessageExceedingColumnLimit_acceptedAtJavaLevel() throws Exception {
        String exceedingMessage = "E".repeat(3000);
        var exception = new DataAccessResourceFailureException(exceedingMessage);

        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        handler.recover(exception, new SendCourseToApproveIntegrationEvent(COURSE_ID));

        assertPersistedExceptionMessage(repo, exceedingMessage);
    }

    // --- Helpers ---

    private DataAccessResourceFailureException createDeeplyNestedException(String topMessage) {
        var root = new RuntimeException("root cause: connection reset");
        var mid1 = new RuntimeException("mid-level: retry exhausted", root);
        var mid2 = new RuntimeException("mid-level: transaction rolled back", mid1);
        return new DataAccessResourceFailureException(topMessage, mid2);
    }

    private void assertPersistedExceptionMessage(FailedIntegrationEventRepository repo,
                                                  String expectedMessage) throws Exception {
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionMessage");
        field.setAccessible(true);
        assertThat(field.get(captor.getValue())).isEqualTo(expectedMessage);
    }
}
