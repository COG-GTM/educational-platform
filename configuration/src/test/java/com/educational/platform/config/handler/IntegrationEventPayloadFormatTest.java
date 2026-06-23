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
 * Verifies the toString() format stability of all integration event records.
 * <p>
 * The @Recover methods persist {@code event.toString()} as the dead-letter payload
 * in {@code FailedIntegrationEventRecord.eventPayload}. If the record format ever
 * changes (e.g., field renamed, record converted to class without matching toString),
 * the dead-letter records become unreadable or unparseable by operators.
 * <p>
 * These tests guard against accidental format changes and ensure the persisted payload
 * contains all the information needed to diagnose and replay failed events.
 */
class IntegrationEventPayloadFormatTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void sendCourseToApproveEvent_toStringContainsCourseId() {
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);

        String payload = event.toString();

        assertThat(payload)
                .contains("SendCourseToApproveIntegrationEvent")
                .contains("courseId")
                .contains(COURSE_ID.toString());
    }

    @Test
    void sendCourseToApproveEvent_toStringFollowsRecordFormat() {
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);

        String payload = event.toString();

        assertThat(payload).isEqualTo(
                "SendCourseToApproveIntegrationEvent[courseId=%s]".formatted(COURSE_ID));
    }

    @Test
    void courseApprovedByAdminEvent_toStringContainsCourseId() {
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);

        String payload = event.toString();

        assertThat(payload)
                .contains("CourseApprovedByAdminIntegrationEvent")
                .contains("courseId")
                .contains(COURSE_ID.toString());
    }

    @Test
    void courseApprovedByAdminEvent_toStringFollowsRecordFormat() {
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);

        String payload = event.toString();

        assertThat(payload).isEqualTo(
                "CourseApprovedByAdminIntegrationEvent[courseId=%s]".formatted(COURSE_ID));
    }

    @Test
    void studentEnrolledToCourseEvent_toStringContainsCourseIdAndUsername() {
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");

        String payload = event.toString();

        assertThat(payload)
                .contains("StudentEnrolledToCourseIntegrationEvent")
                .contains("courseId")
                .contains(COURSE_ID.toString())
                .contains("username")
                .contains("student1");
    }

    @Test
    void studentEnrolledToCourseEvent_toStringFollowsRecordFormat() {
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");

        String payload = event.toString();

        assertThat(payload).isEqualTo(
                "StudentEnrolledToCourseIntegrationEvent[courseId=%s, username=student1]".formatted(COURSE_ID));
    }

    @Test
    void courseRatingRecalculatedEvent_toStringContainsCourseIdAndRating() {
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);

        String payload = event.toString();

        assertThat(payload)
                .contains("CourseRatingRecalculatedIntegrationEvent")
                .contains("courseId")
                .contains(COURSE_ID.toString())
                .contains("rating")
                .contains("4.5");
    }

    @Test
    void courseRatingRecalculatedEvent_toStringFollowsRecordFormat() {
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);

        String payload = event.toString();

        assertThat(payload).isEqualTo(
                "CourseRatingRecalculatedIntegrationEvent[courseId=%s, rating=4.5]".formatted(COURSE_ID));
    }

    @Test
    void userCreatedEvent_toStringContainsUsernameAndEmail() {
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");

        String payload = event.toString();

        assertThat(payload)
                .contains("UserCreatedIntegrationEvent")
                .contains("username")
                .contains("teacher1")
                .contains("email")
                .contains("teacher1@test.com");
    }

    @Test
    void userCreatedEvent_toStringFollowsRecordFormat() {
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");

        String payload = event.toString();

        assertThat(payload).isEqualTo(
                "UserCreatedIntegrationEvent[username=teacher1, email=teacher1@test.com]");
    }

    @Test
    void sendCourseToApproveEvent_toStringWithNullCourseId_containsNull() {
        var event = new SendCourseToApproveIntegrationEvent(null);

        String payload = event.toString();

        assertThat(payload)
                .contains("SendCourseToApproveIntegrationEvent")
                .contains("courseId=null");
    }

    @Test
    void studentEnrolledToCourseEvent_toStringWithNullUsername_containsNull() {
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, null);

        String payload = event.toString();

        assertThat(payload)
                .contains("username=null")
                .contains(COURSE_ID.toString());
    }

    @Test
    void courseRatingRecalculatedEvent_toStringWithNaN_containsNaN() {
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, Double.NaN);

        String payload = event.toString();

        assertThat(payload).contains("NaN");
    }

    @Test
    void courseRatingRecalculatedEvent_toStringWithInfinity_containsInfinity() {
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, Double.POSITIVE_INFINITY);

        String payload = event.toString();

        assertThat(payload).contains("Infinity");
    }

    @Test
    void userCreatedEvent_toStringWithUnicodeUsername_preservesUnicode() {
        var event = new UserCreatedIntegrationEvent("日本語ユーザー", "unicode@test.com");

        String payload = event.toString();

        assertThat(payload)
                .contains("日本語ユーザー")
                .contains("unicode@test.com");
    }

    @Test
    void userCreatedEvent_toStringWithEmptyFields_containsEmptyStrings() {
        var event = new UserCreatedIntegrationEvent("", "");

        String payload = event.toString();

        assertThat(payload).isEqualTo("UserCreatedIntegrationEvent[username=, email=]");
    }

    @Test
    void allEvents_toStringStartsWithClassName() {
        assertThat(new SendCourseToApproveIntegrationEvent(COURSE_ID).toString())
                .startsWith("SendCourseToApproveIntegrationEvent[");
        assertThat(new CourseApprovedByAdminIntegrationEvent(COURSE_ID).toString())
                .startsWith("CourseApprovedByAdminIntegrationEvent[");
        assertThat(new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "user").toString())
                .startsWith("StudentEnrolledToCourseIntegrationEvent[");
        assertThat(new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5).toString())
                .startsWith("CourseRatingRecalculatedIntegrationEvent[");
        assertThat(new UserCreatedIntegrationEvent("user", "email").toString())
                .startsWith("UserCreatedIntegrationEvent[");
    }

    @Test
    void allEvents_toStringEndsWithClosingBracket() {
        assertThat(new SendCourseToApproveIntegrationEvent(COURSE_ID).toString())
                .endsWith("]");
        assertThat(new CourseApprovedByAdminIntegrationEvent(COURSE_ID).toString())
                .endsWith("]");
        assertThat(new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "user").toString())
                .endsWith("]");
        assertThat(new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5).toString())
                .endsWith("]");
        assertThat(new UserCreatedIntegrationEvent("user", "email").toString())
                .endsWith("]");
    }

    @Test
    void allEvents_toStringIsNotNull() {
        assertThat(new SendCourseToApproveIntegrationEvent(COURSE_ID).toString()).isNotNull();
        assertThat(new CourseApprovedByAdminIntegrationEvent(COURSE_ID).toString()).isNotNull();
        assertThat(new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "user").toString()).isNotNull();
        assertThat(new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5).toString()).isNotNull();
        assertThat(new UserCreatedIntegrationEvent("user", "email").toString()).isNotNull();
    }

    @Test
    void courseRatingRecalculatedEvent_toStringWithZeroRating() {
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 0.0);

        assertThat(event.toString()).contains("rating=0.0");
    }

    @Test
    void courseRatingRecalculatedEvent_toStringWithNegativeRating() {
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, -1.0);

        assertThat(event.toString()).contains("rating=-1.0");
    }

    @Test
    void studentEnrolledToCourseEvent_toStringWithSpecialCharsInUsername() {
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "user@domain.com");

        String payload = event.toString();

        assertThat(payload).contains("username=user@domain.com");
    }
}
