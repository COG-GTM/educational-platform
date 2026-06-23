package com.educational.platform.config.handler;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the fully-qualified class names of all integration events.
 * <p>
 * The handler {@code @Recover} methods persist {@code event.getClass().getName()} as
 * the {@code eventClassName} column in the dead-letter table. If an event class is
 * relocated to a different package (or renamed), existing dead-letter records become
 * unresolvable—operators cannot correlate them back to the originating event type.
 * <p>
 * These tests act as a guardrail: if a refactoring changes an event's FQN, this test
 * forces the developer to acknowledge the schema-level impact.
 */
class IntegrationEventClassNameStabilityTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void sendCourseToApproveEvent_fqnIsStable() {
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        assertThat(event.getClass().getName())
                .isEqualTo("com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent");
    }

    @Test
    void courseApprovedByAdminEvent_fqnIsStable() {
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        assertThat(event.getClass().getName())
                .isEqualTo("com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent");
    }

    @Test
    void studentEnrolledToCourseEvent_fqnIsStable() {
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        assertThat(event.getClass().getName())
                .isEqualTo("com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent");
    }

    @Test
    void courseRatingRecalculatedEvent_fqnIsStable() {
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        assertThat(event.getClass().getName())
                .isEqualTo("com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent");
    }

    @Test
    void userCreatedEvent_fqnIsStable() {
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        assertThat(event.getClass().getName())
                .isEqualTo("com.educational.platform.users.integration.event.UserCreatedIntegrationEvent");
    }

    @Test
    void allEvents_fqnStartsWithPlatformPackage() {
        assertThat(SendCourseToApproveIntegrationEvent.class.getName())
                .startsWith("com.educational.platform.");
        assertThat(CourseApprovedByAdminIntegrationEvent.class.getName())
                .startsWith("com.educational.platform.");
        assertThat(StudentEnrolledToCourseIntegrationEvent.class.getName())
                .startsWith("com.educational.platform.");
        assertThat(CourseRatingRecalculatedIntegrationEvent.class.getName())
                .startsWith("com.educational.platform.");
        assertThat(UserCreatedIntegrationEvent.class.getName())
                .startsWith("com.educational.platform.");
    }

    @Test
    void allEvents_fqnContainsIntegrationEventSegment() {
        assertThat(SendCourseToApproveIntegrationEvent.class.getName())
                .contains(".integration.event.");
        assertThat(CourseApprovedByAdminIntegrationEvent.class.getName())
                .contains(".integration.event.");
        assertThat(StudentEnrolledToCourseIntegrationEvent.class.getName())
                .contains(".integration.event.");
        assertThat(CourseRatingRecalculatedIntegrationEvent.class.getName())
                .contains(".integration.event.");
        assertThat(UserCreatedIntegrationEvent.class.getName())
                .contains(".integration.event.");
    }

    @Test
    void allEvents_fqnFitsWithinEventClassNameColumnLimit() {
        int columnLimit = 500;
        assertThat(SendCourseToApproveIntegrationEvent.class.getName().length())
                .isLessThanOrEqualTo(columnLimit);
        assertThat(CourseApprovedByAdminIntegrationEvent.class.getName().length())
                .isLessThanOrEqualTo(columnLimit);
        assertThat(StudentEnrolledToCourseIntegrationEvent.class.getName().length())
                .isLessThanOrEqualTo(columnLimit);
        assertThat(CourseRatingRecalculatedIntegrationEvent.class.getName().length())
                .isLessThanOrEqualTo(columnLimit);
        assertThat(UserCreatedIntegrationEvent.class.getName().length())
                .isLessThanOrEqualTo(columnLimit);
    }
}
