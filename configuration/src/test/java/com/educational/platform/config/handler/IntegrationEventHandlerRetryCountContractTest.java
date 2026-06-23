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
 * Verifies the retry count contract across all integration event handlers:
 * the {@code retryCount} persisted in {@link FailedIntegrationEventRecord} is always
 * the handler's {@code MAX_ATTEMPTS} constant, documenting the design decision that
 * the dead-letter record stores the configured maximum retry count rather than the
 * actual number of attempts made before exhaustion.
 * <p>
 * This is an important semantic contract: the value represents "how many times was the
 * system configured to retry" rather than "how many times did it actually try". If a
 * handler's MAX_ATTEMPTS changes, the persisted value changes accordingly.
 */
class IntegrationEventHandlerRetryCountContractTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static final int EXPECTED_MAX_ATTEMPTS = 3;

    @Test
    void sendCourseToApproveHandler_recover_persistsMaxAttemptsAsRetryCount() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat(getField(captor.getValue(), "retryCount"))
                .as("retryCount must equal MAX_ATTEMPTS constant")
                .isEqualTo(EXPECTED_MAX_ATTEMPTS);
    }

    @Test
    void courseApprovedByAdminHandler_recover_persistsMaxAttemptsAsRetryCount() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat(getField(captor.getValue(), "retryCount"))
                .as("retryCount must equal MAX_ATTEMPTS constant")
                .isEqualTo(EXPECTED_MAX_ATTEMPTS);
    }

    @Test
    void studentEnrolledToCourseHandler_recover_persistsMaxAttemptsAsRetryCount() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat(getField(captor.getValue(), "retryCount"))
                .as("retryCount must equal MAX_ATTEMPTS constant")
                .isEqualTo(EXPECTED_MAX_ATTEMPTS);
    }

    @Test
    void courseRatingRecalculatedHandler_recover_persistsMaxAttemptsAsRetryCount() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat(getField(captor.getValue(), "retryCount"))
                .as("retryCount must equal MAX_ATTEMPTS constant")
                .isEqualTo(EXPECTED_MAX_ATTEMPTS);
    }

    @Test
    void userCreatedHandler_recover_persistsMaxAttemptsAsRetryCount() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        var exception = new DataAccessResourceFailureException("DB error");

        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat(getField(captor.getValue(), "retryCount"))
                .as("retryCount must equal MAX_ATTEMPTS constant")
                .isEqualTo(EXPECTED_MAX_ATTEMPTS);
    }

    @Test
    void allHandlers_maxAttemptsConstant_hasExpectedValue() throws Exception {
        var handlerClasses = java.util.List.of(
                SendCourseToApproveIntegrationEventHandler.class,
                CourseApprovedByAdminIntegrationEventHandler.class,
                StudentEnrolledToCourseIntegrationEventHandler.class,
                CourseRatingRecalculatedIntegrationEventHandler.class,
                UserCreatedIntegrationEventHandler.class
        );

        for (Class<?> handlerClass : handlerClasses) {
            Field field = handlerClass.getDeclaredField("MAX_ATTEMPTS");
            field.setAccessible(true);
            int maxAttempts = (int) field.get(null);
            assertThat(maxAttempts)
                    .as("MAX_ATTEMPTS in %s must match expected value", handlerClass.getSimpleName())
                    .isEqualTo(EXPECTED_MAX_ATTEMPTS);
        }
    }

    @Test
    void retryCountInRecord_matchesRetryableMaxAttempts_notActualAttemptsMade() throws Exception {
        // This test documents that MAX_ATTEMPTS is what gets persisted — regardless of
        // whether fewer attempts were actually made (e.g., if the handler is invoked
        // directly without Spring Retry proxy). The contract is: recover() always records
        // MAX_ATTEMPTS because Spring Retry guarantees it only calls @Recover after
        // all configured attempts are exhausted.
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var exception = new DataAccessResourceFailureException("DB error");

        // Calling recover directly (bypassing Spring Retry proxy) still records MAX_ATTEMPTS
        handler.recover(exception, event);

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        int persistedRetryCount = (int) getField(captor.getValue(), "retryCount");
        assertThat(persistedRetryCount)
                .as("recover() persists the configured MAX_ATTEMPTS, not actual attempt count")
                .isEqualTo(EXPECTED_MAX_ATTEMPTS);
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
