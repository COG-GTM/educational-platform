package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseCannotBePublishedExceptionTest {

    @Test
    void getMessage_containsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseCannotBePublishedException sut = new CourseCannotBePublishedException(uuid);

        // then
        assertThat(sut.getMessage()).contains("123e4567-e89b-12d3-a456-426655440001");
    }

    @Test
    void getMessage_containsExpectedText() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseCannotBePublishedException sut = new CourseCannotBePublishedException(uuid);

        // then
        assertThat(sut.getMessage())
                .isEqualTo("Course with uuid = 123e4567-e89b-12d3-a456-426655440001 cannot be published, course should be approved by admin at first");
    }

    @Test
    void isRuntimeException() {
        // then
        assertThat(RuntimeException.class).isAssignableFrom(CourseCannotBePublishedException.class);
    }

    @Test
    void constructor_nullUuid_messageContainsNull() {
        // when
        final CourseCannotBePublishedException sut = new CourseCannotBePublishedException(null);

        // then
        assertThat(sut.getMessage()).contains("null");
    }
}
