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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateStudentCommandHandlerTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private CreateStudentCommandHandler sut;

    @Test
    void handle_newUsername_studentSavedWithUuidAndUsername() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CreateStudentCommand command = new CreateStudentCommand(uuid, "username");
        when(studentRepository.existsByUsername("username")).thenReturn(false);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Student> argument = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(argument.capture());
        final Student student = argument.getValue();
        assertThat(student)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("username", "username");
        assertThat(student.toReference()).isEqualTo(uuid);
    }

    @Test
    void handle_usernameAlreadyExists_nothingSaved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CreateStudentCommand command = new CreateStudentCommand(uuid, "username");
        when(studentRepository.existsByUsername("username")).thenReturn(true);

        // when
        sut.handle(command);

        // then
        verify(studentRepository, never()).save(any());
    }

    @Test
    void handle_nullUuid_studentSavedWithNullReference() {
        // given
        final CreateStudentCommand command = new CreateStudentCommand(null, "username");
        when(studentRepository.existsByUsername("username")).thenReturn(false);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Student> argument = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(argument.capture());
        assertThat(argument.getValue().toReference()).isNull();
    }
}
