package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests exception message formatting for {@link CourseAlreadyApprovedException}
 * and {@link CourseCannotBePublishedException}.
 */
public class CourseExceptionsMessageTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

    @Test
    void courseAlreadyApprovedException_messageContainsUuid() {
        // when
        final CourseAlreadyApprovedException exception = new CourseAlreadyApprovedException(UUID_VALUE);

        // then
        assertThat(exception.getMessage())
                .contains(UUID_VALUE.toString())
                .contains("cannot be sent for approval")
                .contains("already approved");
    }

    @Test
    void courseAlreadyApprovedException_isRuntimeException() {
        final CourseAlreadyApprovedException exception = new CourseAlreadyApprovedException(UUID_VALUE);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void courseCannotBePublishedException_messageContainsUuid() {
        // when
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(UUID_VALUE);

        // then
        assertThat(exception.getMessage())
                .contains(UUID_VALUE.toString())
                .contains("cannot be published")
                .contains("approved by admin");
    }

    @Test
    void courseCannotBePublishedException_isRuntimeException() {
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(UUID_VALUE);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void courseAlreadyApprovedException_nullUuid_messageContainsNull() {
        // when
        final CourseAlreadyApprovedException exception = new CourseAlreadyApprovedException(null);

        // then
        assertThat(exception.getMessage()).contains("null");
    }

    @Test
    void courseCannotBePublishedException_nullUuid_messageContainsNull() {
        // when
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(null);

        // then
        assertThat(exception.getMessage()).contains("null");
    }
}
