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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Verifies the null-event contract across all integration event handlers.
 * <p>
 * Handlers do not null-check the event parameter — they rely on Spring's
 * {@code @EventListener} contract which guarantees non-null events. When a null
 * event is passed directly (bypassing Spring), a {@link NullPointerException}
 * is thrown when accessing event fields (e.g., {@code event.courseId()}).
 * <p>
 * These tests document this contract and verify consistent behavior across handlers.
 */
class IntegrationEventHandlerNullEventContractTest {

    @Test
    void sendCourseToApproveHandler_nullEvent_throwsNullPointerException() {
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class),
                mock(FailedIntegrationEventRepository.class));

        assertThatThrownBy(() -> handler.handleSendCourseToApproveEvent(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void courseApprovedByAdminHandler_nullEvent_throwsNullPointerException() {
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class),
                mock(FailedIntegrationEventRepository.class));

        assertThatThrownBy(() -> handler.handleCourseApprovedByAdminEvent(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void studentEnrolledToCourseHandler_nullEvent_throwsNullPointerException() {
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class),
                mock(FailedIntegrationEventRepository.class));

        assertThatThrownBy(() -> handler.handleStudentEnrolledToCourseEvent(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void courseRatingRecalculatedHandler_nullEvent_throwsNullPointerException() {
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class),
                mock(FailedIntegrationEventRepository.class));

        assertThatThrownBy(() -> handler.handleCourseRatingRecalculatedEvent(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void userCreatedHandler_nullEvent_throwsNullPointerException() {
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class),
                mock(FailedIntegrationEventRepository.class));

        assertThatThrownBy(() -> handler.handleUserCreatedEvent(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void allHandlers_nullEvent_doesNotInteractWithRepository() {
        var repo1 = mock(FailedIntegrationEventRepository.class);
        var repo2 = mock(FailedIntegrationEventRepository.class);
        var repo3 = mock(FailedIntegrationEventRepository.class);
        var repo4 = mock(FailedIntegrationEventRepository.class);
        var repo5 = mock(FailedIntegrationEventRepository.class);

        try { new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo1)
                .handleSendCourseToApproveEvent(null); } catch (NullPointerException ignored) {}
        try { new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo2)
                .handleCourseApprovedByAdminEvent(null); } catch (NullPointerException ignored) {}
        try { new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo3)
                .handleStudentEnrolledToCourseEvent(null); } catch (NullPointerException ignored) {}
        try { new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo4)
                .handleCourseRatingRecalculatedEvent(null); } catch (NullPointerException ignored) {}
        try { new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo5)
                .handleUserCreatedEvent(null); } catch (NullPointerException ignored) {}

        verifyNoInteractions(repo1, repo2, repo3, repo4, repo5);
    }
}
