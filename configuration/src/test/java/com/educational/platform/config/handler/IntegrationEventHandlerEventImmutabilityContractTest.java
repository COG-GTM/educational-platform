package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.CreateCourseProposalCommandHandler;
import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.courses.course.approve.ApproveCourseCommandHandler;
import com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import com.educational.platform.courses.teacher.create.CreateTeacherCommandHandler;
import com.educational.platform.courses.teacher.create.UserCreatedIntegrationEventHandler;
import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies that integration event objects remain unchanged after being processed
 * by handler methods and recover methods.
 * <p>
 * Since all events are Java records (immutable by design), this serves as a
 * contract test ensuring that the handler pattern does not depend on mutable events
 * and that the same event instance can safely be passed to both {@code handleEvent()}
 * and {@code recover()} without side effects.
 */
class IntegrationEventHandlerEventImmutabilityContractTest {

    @Test
    void sendCourseToApproveEvent_unchangedAfterHandleAndRecover() {
        // given
        var uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        var event = new SendCourseToApproveIntegrationEvent(uuid);
        var toStringBefore = event.toString();
        var hashCodeBefore = event.hashCode();
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class),
                mock(FailedIntegrationEventRepository.class));

        // when — process event through handler
        handler.handleSendCourseToApproveEvent(event);

        // then — event is unchanged
        assertThat(event.courseId()).isEqualTo(uuid);
        assertThat(event.toString()).isEqualTo(toStringBefore);
        assertThat(event.hashCode()).isEqualTo(hashCodeBefore);
    }

    @Test
    void courseApprovedByAdminEvent_unchangedAfterRecover() {
        // given
        var uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        var event = new CourseApprovedByAdminIntegrationEvent(uuid);
        var toStringBefore = event.toString();
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class),
                mock(FailedIntegrationEventRepository.class));
        var exception = new DataAccessResourceFailureException("DB error");

        // when — process event through recover
        handler.recover(exception, event);

        // then — event is unchanged
        assertThat(event.courseId()).isEqualTo(uuid);
        assertThat(event.toString()).isEqualTo(toStringBefore);
    }

    @Test
    void studentEnrolledToCourseEvent_unchangedAfterHandleAndRecover() {
        // given
        var uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        var event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student@example.com");
        var toStringBefore = event.toString();
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class),
                mock(FailedIntegrationEventRepository.class));

        // when — process through handler
        handler.handleStudentEnrolledToCourseEvent(event);

        // then
        assertThat(event.courseId()).isEqualTo(uuid);
        assertThat(event.username()).isEqualTo("student@example.com");
        assertThat(event.toString()).isEqualTo(toStringBefore);

        // when — then process through recover with the SAME event instance
        handler.recover(new DataAccessResourceFailureException("DB error"), event);

        // then — still unchanged
        assertThat(event.courseId()).isEqualTo(uuid);
        assertThat(event.username()).isEqualTo("student@example.com");
        assertThat(event.toString()).isEqualTo(toStringBefore);
    }

    @Test
    void courseRatingRecalculatedEvent_unchangedAfterHandleAndRecover() {
        // given
        var uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        var event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.75);
        var toStringBefore = event.toString();
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class),
                mock(FailedIntegrationEventRepository.class));

        // when
        handler.handleCourseRatingRecalculatedEvent(event);
        handler.recover(new DataAccessResourceFailureException("DB error"), event);

        // then — record fields are immutable
        assertThat(event.courseId()).isEqualTo(uuid);
        assertThat(event.rating()).isEqualTo(4.75);
        assertThat(event.toString()).isEqualTo(toStringBefore);
    }

    @Test
    void userCreatedEvent_unchangedAfterHandleAndRecover() {
        // given
        var event = new UserCreatedIntegrationEvent("john.doe", "john@example.com");
        var toStringBefore = event.toString();
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class),
                mock(FailedIntegrationEventRepository.class));

        // when
        handler.handleUserCreatedEvent(event);
        handler.recover(new DataAccessResourceFailureException("DB error"), event);

        // then
        assertThat(event.username()).isEqualTo("john.doe");
        assertThat(event.email()).isEqualTo("john@example.com");
        assertThat(event.toString()).isEqualTo(toStringBefore);
    }

    @Test
    void sameEventInstance_canBeUsedForMultipleRecoverCalls() {
        // given — simulates the scenario where Spring Retry passes the same event to recover
        var uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        var event = new SendCourseToApproveIntegrationEvent(uuid);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class),
                mock(FailedIntegrationEventRepository.class));
        var exception1 = new DataAccessResourceFailureException("first failure");
        var exception2 = new DataAccessResourceFailureException("second failure");

        // when — recover called twice with the same event
        handler.recover(exception1, event);
        handler.recover(exception2, event);

        // then — event still valid for use
        assertThat(event.courseId()).isEqualTo(uuid);
        assertThat(event.toString()).contains(uuid.toString());
    }
}
