package com.educational.platform.courses.course;

import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseIntegrationEventsTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void sendCourseToApproveIntegrationEvent_exposesCourseId() {
        // when
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(UUID_VALUE);

        // then
        assertThat(event.courseId()).isEqualTo(UUID_VALUE);
    }

    @Test
    void sendCourseToApproveIntegrationEvent_equalInstances() {
        assertThat(new SendCourseToApproveIntegrationEvent(UUID_VALUE))
                .isEqualTo(new SendCourseToApproveIntegrationEvent(UUID_VALUE));
    }

    @Test
    void sendCourseToApproveIntegrationEvent_differentUuids_notEqual() {
        assertThat(new SendCourseToApproveIntegrationEvent(UUID_VALUE))
                .isNotEqualTo(new SendCourseToApproveIntegrationEvent(UUID.randomUUID()));
    }

    @Test
    void courseRatingRecalculatedIntegrationEvent_exposesCourseIdAndRating() {
        // when
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(UUID_VALUE, 4.5);

        // then
        assertThat(event.courseId()).isEqualTo(UUID_VALUE);
        assertThat(event.rating()).isEqualTo(4.5);
    }

    @Test
    void courseRatingRecalculatedIntegrationEvent_equalInstances() {
        assertThat(new CourseRatingRecalculatedIntegrationEvent(UUID_VALUE, 4.5))
                .isEqualTo(new CourseRatingRecalculatedIntegrationEvent(UUID_VALUE, 4.5));
    }

    @Test
    void courseRatingRecalculatedIntegrationEvent_differentRating_notEqual() {
        assertThat(new CourseRatingRecalculatedIntegrationEvent(UUID_VALUE, 4.5))
                .isNotEqualTo(new CourseRatingRecalculatedIntegrationEvent(UUID_VALUE, 3.0));
    }

    @Test
    void userCreatedIntegrationEvent_exposesUsernameAndEmail() {
        // when
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");

        // then
        assertThat(event.username()).isEqualTo("username");
        assertThat(event.email()).isEqualTo("user@example.com");
    }

    @Test
    void userCreatedIntegrationEvent_equalInstances() {
        assertThat(new UserCreatedIntegrationEvent("username", "email@test.com"))
                .isEqualTo(new UserCreatedIntegrationEvent("username", "email@test.com"));
    }

    @Test
    void userCreatedIntegrationEvent_differentValues_notEqual() {
        assertThat(new UserCreatedIntegrationEvent("user1", "email@test.com"))
                .isNotEqualTo(new UserCreatedIntegrationEvent("user2", "email@test.com"));
    }
}
