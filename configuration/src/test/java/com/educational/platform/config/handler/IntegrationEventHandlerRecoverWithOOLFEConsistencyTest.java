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
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Cross-cutting test verifying that all handlers' {@code recover()} method works
 * correctly when invoked with {@link ObjectOptimisticLockingFailureException}.
 * <p>
 * The existing {@code IntegrationEventHandlerRecoverBoundaryTest} only uses
 * {@link org.springframework.dao.DataAccessResourceFailureException}. This test
 * complements it by exercising the other retryFor exception type, ensuring the
 * persisted {@code exceptionClassName} reflects the actual OOLFE class — not a
 * superclass or fallback.
 */
class IntegrationEventHandlerRecoverWithOOLFEConsistencyTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static final String OOLFE_CLASS_NAME = ObjectOptimisticLockingFailureException.class.getName();

    @Test
    void sendCourseToApproveHandler_recover_withOOLFE_persistsCorrectExceptionClassName() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var exception = new ObjectOptimisticLockingFailureException("optimistic lock on course", new RuntimeException());

        handler.recover(exception, event);

        assertPersistedExceptionClassName(repo, OOLFE_CLASS_NAME);
    }

    @Test
    void courseApprovedByAdminHandler_recover_withOOLFE_persistsCorrectExceptionClassName() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        var exception = new ObjectOptimisticLockingFailureException("optimistic lock on approval", new RuntimeException());

        handler.recover(exception, event);

        assertPersistedExceptionClassName(repo, OOLFE_CLASS_NAME);
    }

    @Test
    void studentEnrolledToCourseHandler_recover_withOOLFE_persistsCorrectExceptionClassName() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        var exception = new ObjectOptimisticLockingFailureException("optimistic lock on enrollment", new RuntimeException());

        handler.recover(exception, event);

        assertPersistedExceptionClassName(repo, OOLFE_CLASS_NAME);
    }

    @Test
    void courseRatingRecalculatedHandler_recover_withOOLFE_persistsCorrectExceptionClassName() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        var exception = new ObjectOptimisticLockingFailureException("optimistic lock on rating", new RuntimeException());

        handler.recover(exception, event);

        assertPersistedExceptionClassName(repo, OOLFE_CLASS_NAME);
    }

    @Test
    void userCreatedHandler_recover_withOOLFE_persistsCorrectExceptionClassName() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        var exception = new ObjectOptimisticLockingFailureException("optimistic lock on teacher", new RuntimeException());

        handler.recover(exception, event);

        assertPersistedExceptionClassName(repo, OOLFE_CLASS_NAME);
    }

    @Test
    void allHandlers_recover_withOOLFE_persistCorrectRetryCount() throws Exception {
        var exception = new ObjectOptimisticLockingFailureException("lock conflict", new RuntimeException());

        var repo1 = mock(FailedIntegrationEventRepository.class);
        new SendCourseToApproveIntegrationEventHandler(mock(CreateCourseProposalCommandHandler.class), repo1)
                .recover(exception, new SendCourseToApproveIntegrationEvent(COURSE_ID));

        var repo2 = mock(FailedIntegrationEventRepository.class);
        new CourseApprovedByAdminIntegrationEventHandler(mock(ApproveCourseCommandHandler.class), repo2)
                .recover(exception, new CourseApprovedByAdminIntegrationEvent(COURSE_ID));

        var repo3 = mock(FailedIntegrationEventRepository.class);
        new StudentEnrolledToCourseIntegrationEventHandler(mock(IncreaseNumberOfStudentsCommandHandler.class), repo3)
                .recover(exception, new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student"));

        var repo4 = mock(FailedIntegrationEventRepository.class);
        new CourseRatingRecalculatedIntegrationEventHandler(mock(UpdateCourseRatingCommandHandler.class), repo4)
                .recover(exception, new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 3.0));

        var repo5 = mock(FailedIntegrationEventRepository.class);
        new UserCreatedIntegrationEventHandler(mock(CreateTeacherCommandHandler.class), repo5)
                .recover(exception, new UserCreatedIntegrationEvent("user", "user@test.com"));

        for (var repo : new FailedIntegrationEventRepository[]{repo1, repo2, repo3, repo4, repo5}) {
            var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
            verify(repo).save(captor.capture());
            Field retryCountField = FailedIntegrationEventRecord.class.getDeclaredField("retryCount");
            retryCountField.setAccessible(true);
            assertThat((int) retryCountField.get(captor.getValue()))
                    .as("retryCount should be MAX_ATTEMPTS (3)")
                    .isEqualTo(3);
        }
    }

    @Test
    void allHandlers_recover_withOOLFE_persistFailedStatus() throws Exception {
        var exception = new ObjectOptimisticLockingFailureException("lock conflict", new RuntimeException());

        var repo1 = mock(FailedIntegrationEventRepository.class);
        new SendCourseToApproveIntegrationEventHandler(mock(CreateCourseProposalCommandHandler.class), repo1)
                .recover(exception, new SendCourseToApproveIntegrationEvent(COURSE_ID));

        var repo2 = mock(FailedIntegrationEventRepository.class);
        new CourseApprovedByAdminIntegrationEventHandler(mock(ApproveCourseCommandHandler.class), repo2)
                .recover(exception, new CourseApprovedByAdminIntegrationEvent(COURSE_ID));

        var repo3 = mock(FailedIntegrationEventRepository.class);
        new StudentEnrolledToCourseIntegrationEventHandler(mock(IncreaseNumberOfStudentsCommandHandler.class), repo3)
                .recover(exception, new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student"));

        var repo4 = mock(FailedIntegrationEventRepository.class);
        new CourseRatingRecalculatedIntegrationEventHandler(mock(UpdateCourseRatingCommandHandler.class), repo4)
                .recover(exception, new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 3.0));

        var repo5 = mock(FailedIntegrationEventRepository.class);
        new UserCreatedIntegrationEventHandler(mock(CreateTeacherCommandHandler.class), repo5)
                .recover(exception, new UserCreatedIntegrationEvent("user", "user@test.com"));

        for (var repo : new FailedIntegrationEventRepository[]{repo1, repo2, repo3, repo4, repo5}) {
            var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
            verify(repo).save(captor.capture());
            Field statusField = FailedIntegrationEventRecord.class.getDeclaredField("status");
            statusField.setAccessible(true);
            assertThat((FailedIntegrationEventRecord.Status) statusField.get(captor.getValue()))
                    .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
        }
    }

    private void assertPersistedExceptionClassName(FailedIntegrationEventRepository repo,
                                                    String expectedClassName) throws Exception {
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionClassName");
        field.setAccessible(true);
        assertThat(field.get(captor.getValue())).isEqualTo(expectedClassName);
    }
}
