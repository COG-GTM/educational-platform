package com.educational.platform.course.enrollments.student;

import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class StudentTest {

    @Test
    void toReference_uuidReturned() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440101");
        final Student student = new Student(new CreateStudentCommand(uuid, "username"));

        // when
        final UUID reference = student.toReference();

        // then
        assertThat(reference).isEqualTo(uuid);
    }

    @Test
    void assignUuid_legacyStudentWithoutUuid_referenceReturnsAssignedUuid() {
        // given
        final Student student = new Student(new CreateStudentCommand(null, "username"));
        final UUID userUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440101");

        // when
        student.assignUuid(userUuid);

        // then
        assertThat(student.toReference()).isEqualTo(userUuid);
    }

    @Test
    void constructor_validCommand_uuidAndUsernamePopulated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440101");
        final CreateStudentCommand command = new CreateStudentCommand(uuid, "username");

        // when
        final Student student = new Student(command);

        // then
        assertThat(student)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("username", "username");
    }
}
