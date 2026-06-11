package com.educational.platform.course.enrollments.student.create;

import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.StudentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CreateStudentCommandHandlerTest {

    @Mock
    private StudentRepository studentRepository;

    private CreateStudentCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new CreateStudentCommandHandler(studentRepository);
    }

    @Test
    void handle_validCommand_studentSaved() {
        // given
        final CreateStudentCommand command = new CreateStudentCommand("username");

        // when
        sut.handle(command);

        // then
        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        final Student savedStudent = captor.getValue();
        assertThat(savedStudent.toReference()).isEqualTo("username");
    }

    @Test
    void handle_nullUsername_studentSavedWithNullReference() {
        // given
        final CreateStudentCommand command = new CreateStudentCommand(null);

        // when
        sut.handle(command);

        // then
        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        final Student savedStudent = captor.getValue();
        assertThat(savedStudent.toReference()).isNull();
    }

    @Test
    void handle_emptyUsername_studentSavedWithEmptyReference() {
        // given
        final CreateStudentCommand command = new CreateStudentCommand("");

        // when
        sut.handle(command);

        // then
        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        final Student savedStudent = captor.getValue();
        assertThat(savedStudent.toReference()).isEmpty();
    }

    @Test
    void handle_repositoryThrows_exceptionPropagates() {
        // given
        final CreateStudentCommand command = new CreateStudentCommand("student");
        doThrow(new RuntimeException("save failed")).when(studentRepository).save(any(Student.class));

        // when / then
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("save failed");
    }

    @Test
    void handle_whitespaceUsername_studentSavedWithWhitespace() {
        // given
        final CreateStudentCommand command = new CreateStudentCommand("  ");

        // when
        sut.handle(command);

        // then
        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().toReference()).isEqualTo("  ");
    }

    @Test
    void handle_validCommand_savedStudentHasNullIdBeforePersist() {
        // given
        final CreateStudentCommand command = new CreateStudentCommand("username");

        // when
        sut.handle(command);

        // then
        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
    }
}
