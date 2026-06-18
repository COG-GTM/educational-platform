package com.educational.platform.course.enrollments.student.create;

import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.StudentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateStudentCommandHandlerTest {

    @Mock
    private StudentRepository repository;

    @InjectMocks
    private CreateStudentCommandHandler sut;

    @Test
    void handle_validCommand_studentSaved() {
        // given - the student projection carries the username that the enrollment producer later forwards
        // as the StudentEnrolledToCourse event's reference, so the persisted username must match the command
        final CreateStudentCommand command = new CreateStudentCommand("student");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Student> argument = ArgumentCaptor.forClass(Student.class);
        verify(repository).save(argument.capture());
        final Student saved = argument.getValue();
        assertThat(saved)
                .hasFieldOrPropertyWithValue("username", "student");
        assertThat(saved.toReference()).isEqualTo("student");
    }

    @Test
    void handle_emptyUsername_studentSavedWithEmptyUsername() {
        // given - the handler performs no validation; an empty username is persisted verbatim
        final CreateStudentCommand command = new CreateStudentCommand("");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Student> argument = ArgumentCaptor.forClass(Student.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("username", "");
    }

    @Test
    void handle_nullUsername_studentSavedWithNullUsername() {
        // given - a null username is forwarded verbatim to the persisted student
        final CreateStudentCommand command = new CreateStudentCommand(null);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Student> argument = ArgumentCaptor.forClass(Student.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("username", null);
    }

    @Test
    void handle_repositoryThrows_exceptionPropagated() {
        // given - a persistence failure must propagate rather than be swallowed
        final CreateStudentCommand command = new CreateStudentCommand("student");
        doThrow(new RuntimeException("student could not be saved"))
                .when(repository).save(any(Student.class));

        // when / then
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("student could not be saved");
    }
}
