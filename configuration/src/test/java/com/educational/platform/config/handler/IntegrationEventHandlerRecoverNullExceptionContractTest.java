package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.CreateCourseProposalCommandHandler;
import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Verifies the null-exception contract for all integration event handlers' recover() methods.
 * <p>
 * At runtime with Spring Retry, the exception parameter is never null — it is always the
 * exception that caused recovery after retries are exhausted. However, the recover() method
 * is public and can be invoked directly. These tests document that passing a null exception
 * throws {@link NullPointerException} from the {@code e.getMessage()} / {@code e.getClass().getName()}
 * calls, and that the repository is never touched in this scenario.
 * <p>
 * This is symmetric with the null-event contract tests in
 * {@link IntegrationEventHandlerNullEventContractTest}.
 */
class IntegrationEventHandlerRecoverNullExceptionContractTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void sendCourseToApproveHandler_recover_nullException_throwsNullPointerException() {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);

        assertThatThrownBy(() -> handler.recover(null, event))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void courseApprovedByAdminHandler_recover_nullException_throwsNullPointerException() {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);

        assertThatThrownBy(() -> handler.recover(null, event))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void studentEnrolledToCourseHandler_recover_nullException_throwsNullPointerException() {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");

        assertThatThrownBy(() -> handler.recover(null, event))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void courseRatingRecalculatedHandler_recover_nullException_throwsNullPointerException() {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);

        assertThatThrownBy(() -> handler.recover(null, event))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void userCreatedHandler_recover_nullException_throwsNullPointerException() {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");

        assertThatThrownBy(() -> handler.recover(null, event))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void allHandlers_recover_nullException_doesNotInteractWithRepository() {
        var repo1 = mock(FailedIntegrationEventRepository.class);
        var repo2 = mock(FailedIntegrationEventRepository.class);
        var repo3 = mock(FailedIntegrationEventRepository.class);
        var repo4 = mock(FailedIntegrationEventRepository.class);
        var repo5 = mock(FailedIntegrationEventRepository.class);

        try { new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo1)
                .recover(null, new SendCourseToApproveIntegrationEvent(COURSE_ID)); } catch (NullPointerException ignored) {}
        try { new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo2)
                .recover(null, new CourseApprovedByAdminIntegrationEvent(COURSE_ID)); } catch (NullPointerException ignored) {}
        try { new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo3)
                .recover(null, new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student")); } catch (NullPointerException ignored) {}
        try { new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo4)
                .recover(null, new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5)); } catch (NullPointerException ignored) {}
        try { new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo5)
                .recover(null, new UserCreatedIntegrationEvent("teacher", "t@test.com")); } catch (NullPointerException ignored) {}

        verifyNoInteractions(repo1, repo2, repo3, repo4, repo5);
    }

    @Test
    void allHandlers_recover_nullBothParams_throwsNullPointerException() {
        var repo = mock(FailedIntegrationEventRepository.class);

        assertThatThrownBy(() -> new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo)
                .recover(null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo)
                .recover(null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo)
                .recover(null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo)
                .recover(null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo)
                .recover(null, null))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(repo);
    }
}
