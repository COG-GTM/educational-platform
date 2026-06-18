package com.educational.platform.course.enrollments.course;

import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EnrollCourseTest {

    @Test
    void create_validCommand_courseCreatedWithUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseCommand command = new CreateCourseCommand(uuid);

        // when
        final EnrollCourse course = new EnrollCourse(command);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("uuid", uuid);
        assertThat(course.getId()).isNull();
    }

    @Test
    void toReference_returnsUuid() {
        // given - the uuid is the natural key shared across modules; the projection exposes it via toReference
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(uuid));

        // when
        final UUID reference = course.toReference();

        // then
        assertThat(reference).isEqualTo(uuid);
    }

    @Test
    void create_nullUuid_referenceIsNull() {
        // given - a null uuid is forwarded verbatim, the domain does not reject it
        final CreateCourseCommand command = new CreateCourseCommand(null);

        // when
        final EnrollCourse course = new EnrollCourse(command);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("uuid", null);
        assertThat(course.toReference()).isNull();
    }
}
