package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseCannotBePublishedExceptionTest {

    @Test
    void getMessage_validUuid_messageContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(uuid);

        // when
        final String message = exception.getMessage();

        // then
        assertThat(message).contains(uuid.toString());
        assertThat(message).contains("cannot be published");
        assertThat(message).contains("approved by admin");
    }

    @Test
    void constructor_validUuid_isRuntimeException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseCannotBePublishedException sut = new CourseCannotBePublishedException(uuid);

        // then
        assertThat(sut).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getMessage_validUuid_exactMessage() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseCannotBePublishedException sut = new CourseCannotBePublishedException(uuid);

        // when
        final String message = sut.getMessage();

        // then
        assertThat(message).isEqualTo("Course with uuid = 123e4567-e89b-12d3-a456-426655440001 cannot be published, course should be approved by admin at first");
    }
}
