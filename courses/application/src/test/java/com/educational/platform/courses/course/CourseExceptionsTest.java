package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseExceptionsTest {

    @Test
    void courseAlreadyApprovedException_messageContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseAlreadyApprovedException exception = new CourseAlreadyApprovedException(uuid);

        // then
        assertThat(exception.getMessage())
                .contains(uuid.toString())
                .contains("cannot be sent for approval")
                .contains("already approved");
    }

    @Test
    void courseCannotBePublishedException_messageContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(uuid);

        // then
        assertThat(exception.getMessage())
                .contains(uuid.toString())
                .contains("cannot be published")
                .contains("approved by admin");
    }

    @Test
    void courseAlreadyApprovedException_isRuntimeException() {
        final CourseAlreadyApprovedException exception = new CourseAlreadyApprovedException(UUID.randomUUID());
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void courseCannotBePublishedException_isRuntimeException() {
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(UUID.randomUUID());
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
