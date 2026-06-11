package com.educational.platform.course.enrollments.course;

import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EnrollCourseTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void create_validCommand_uuidStored() {
        // given
        final CreateCourseCommand command = new CreateCourseCommand(UUID_VALUE);

        // when
        final EnrollCourse course = new EnrollCourse(command);

        // then
        assertThat(course.toReference()).isEqualTo(UUID_VALUE);
    }

    @Test
    void getId_freshlyCreated_isNull() {
        final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(UUID_VALUE));
        assertThat(course.getId()).isNull();
    }

    @Test
    void createCourseCommand_exposesUuid() {
        assertThat(new CreateCourseCommand(UUID_VALUE).uuid()).isEqualTo(UUID_VALUE);
    }
}
