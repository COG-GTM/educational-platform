package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests exception behavior when null UUID is passed to course exceptions.
 * Verifies no NPE is thrown during construction and that getMessage handles null gracefully.
 */
public class CourseExceptionsNullUuidTest {

    @Test
    void courseAlreadyApprovedException_nullUuid_messageContainsNull() {
        // when
        final CourseAlreadyApprovedException exception = new CourseAlreadyApprovedException(null);

        // then — getMessage should not throw NPE; "null" is rendered in the message
        assertThat(exception.getMessage())
                .contains("null")
                .contains("cannot be sent for approval");
    }

    @Test
    void courseCannotBePublishedException_nullUuid_messageContainsNull() {
        // when
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(null);

        // then
        assertThat(exception.getMessage())
                .contains("null")
                .contains("cannot be published");
    }

    @Test
    void courseAlreadyApprovedException_nullUuid_isRuntimeException() {
        assertThat(new CourseAlreadyApprovedException(null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void courseCannotBePublishedException_nullUuid_isRuntimeException() {
        assertThat(new CourseCannotBePublishedException(null))
                .isInstanceOf(RuntimeException.class);
    }
}
