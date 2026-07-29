package com.educational.platform.course.enrollments.student;

import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class StudentTest {

    @Test
    void toReference_returnsUuidFromCommand() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final Student sut = new Student(new CreateStudentCommand(uuid, "username"));

        // when
        final UUID reference = sut.toReference();

        // then
        assertThat(reference).isEqualTo(uuid);
    }
}
