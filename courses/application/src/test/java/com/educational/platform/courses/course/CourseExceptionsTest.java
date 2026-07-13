package com.educational.platform.courses.course;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseExceptionsTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void courseCannotBePublishedException_message_containsUuid() {
        // when
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(UUID_VALUE);

        // then
        assertThat(exception.getMessage())
                .isEqualTo("Course with uuid = " + UUID_VALUE + " cannot be published, course should be approved by admin at first");
    }

    @Test
    void courseAlreadyApprovedException_message_containsUuid() {
        // when
        final CourseAlreadyApprovedException exception = new CourseAlreadyApprovedException(UUID_VALUE);

        // then
        assertThat(exception.getMessage())
                .isEqualTo("Course with uuid = " + UUID_VALUE + " cannot be sent for approval, course was already approved");
    }
}
