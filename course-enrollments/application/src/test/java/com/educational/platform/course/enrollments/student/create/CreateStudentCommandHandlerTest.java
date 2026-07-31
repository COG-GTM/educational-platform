package com.educational.platform.course.enrollments.student.create;

import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateStudentCommandHandlerTest {

    @Mock
    private StudentRepository repository;

    @InjectMocks
    private CreateStudentCommandHandler sut;


    @Test
    void handle_newStudent_saveExecuted() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440101");
        final CreateStudentCommand command = new CreateStudentCommand(uuid, "username");
        when(repository.findByUsername("username")).thenReturn(null);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Student> argument = ArgumentCaptor.forClass(Student.class);
        verify(repository).save(argument.capture());
        final Student saved = argument.getValue();
        assertThat(saved)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("username", "username");
    }

    @Test
    void handle_existingStudent_uuidReconciled() {
        // given
        final UUID existingUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440102");
        final UUID userUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440101");
        final Student existing = new Student(new CreateStudentCommand(existingUuid, "username"));
        when(repository.findByUsername("username")).thenReturn(existing);

        // when
        sut.handle(new CreateStudentCommand(userUuid, "username"));

        // then
        final ArgumentCaptor<Student> argument = ArgumentCaptor.forClass(Student.class);
        verify(repository).save(argument.capture());
        final Student saved = argument.getValue();
        assertThat(saved)
                .hasFieldOrPropertyWithValue("uuid", userUuid)
                .hasFieldOrPropertyWithValue("username", "username");
    }

    @Test
    void handle_existingStudent_noNewStudentCreated() {
        // given
        final UUID userUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440101");
        final Student existing = new Student(new CreateStudentCommand(null, "username"));
        when(repository.findByUsername("username")).thenReturn(existing);

        // when
        sut.handle(new CreateStudentCommand(userUuid, "username"));

        // then
        final ArgumentCaptor<Student> argument = ArgumentCaptor.forClass(Student.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue()).isSameAs(existing);
        assertThat(existing.toReference()).isEqualTo(userUuid);
    }
}
