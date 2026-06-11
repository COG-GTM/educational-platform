package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseExceptionsExtrasTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void courseAlreadyApprovedException_messageContainsUuid() {
        // when
        final CourseAlreadyApprovedException exception = new CourseAlreadyApprovedException(UUID_VALUE);

        // then
        assertThat(exception.getMessage())
                .contains(UUID_VALUE.toString())
                .contains("already approved");
    }

    @Test
    void courseCannotBePublishedException_messageContainsUuid() {
        // when
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(UUID_VALUE);

        // then
        assertThat(exception.getMessage())
                .contains(UUID_VALUE.toString())
                .contains("cannot be published");
    }

    @Test
    void courseAlreadyApprovedException_isRuntimeException() {
        assertThat(new CourseAlreadyApprovedException(UUID_VALUE)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void courseCannotBePublishedException_isRuntimeException() {
        assertThat(new CourseCannotBePublishedException(UUID_VALUE)).isInstanceOf(RuntimeException.class);
    }
}
