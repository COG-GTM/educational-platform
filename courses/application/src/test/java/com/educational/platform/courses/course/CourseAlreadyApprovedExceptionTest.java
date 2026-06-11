package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseAlreadyApprovedExceptionTest {

    @Test
    void getMessage_validUuid_messageContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseAlreadyApprovedException exception = new CourseAlreadyApprovedException(uuid);

        // when
        final String message = exception.getMessage();

        // then
        assertThat(message).contains(uuid.toString());
        assertThat(message).contains("cannot be sent for approval");
        assertThat(message).contains("already approved");
    }
}
