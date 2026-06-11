package com.educational.platform.course.enrollments.course;

import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EnrollCourseTest {

    @Test
    void constructor_validCommand_courseCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseCommand command = new CreateCourseCommand(uuid);

        // when
        final EnrollCourse sut = new EnrollCourse(command);

        // then
        assertThat(sut.toReference()).isEqualTo(uuid);
    }

    @Test
    void toReference_returnsUUID() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final EnrollCourse sut = new EnrollCourse(new CreateCourseCommand(uuid));

        // when
        final UUID reference = sut.toReference();

        // then
        assertThat(reference).isEqualTo(uuid);
    }
}
