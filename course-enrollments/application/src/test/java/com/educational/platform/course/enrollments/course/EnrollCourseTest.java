package com.educational.platform.course.enrollments.course;

import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EnrollCourseTest {

    @Test
    void toReference_createdCourse_uuidReturned() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final EnrollCourse sut = new EnrollCourse(new CreateCourseCommand(uuid));

        // when
        final UUID reference = sut.toReference();

        // then
        assertThat(reference).isEqualTo(uuid);
    }

    @Test
    void getId_notPersistedCourse_null() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final EnrollCourse sut = new EnrollCourse(new CreateCourseCommand(uuid));

        // when / then
        assertThat(sut.getId()).isNull();
    }
}
