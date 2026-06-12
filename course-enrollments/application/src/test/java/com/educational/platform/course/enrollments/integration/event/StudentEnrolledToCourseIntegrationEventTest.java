package com.educational.platform.course.enrollments.integration.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class StudentEnrolledToCourseIntegrationEventTest {

    @Test
    void constructor_validArguments_eventCreated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String username = "student";

        // when
        final StudentEnrolledToCourseIntegrationEvent sut = new StudentEnrolledToCourseIntegrationEvent(courseId, username);

        // then
        assertThat(sut.courseId()).isEqualTo(courseId);
        assertThat(sut.username()).isEqualTo(username);
    }
}
