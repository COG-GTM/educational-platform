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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CreateStudentCommandHandlerTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private CreateStudentCommandHandler sut;

    @Test
    void handle_validCommand_studentSaved() {
        // given
        final CreateStudentCommand command = new CreateStudentCommand("username");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Student> argument = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(argument.capture());
        final Student savedStudent = argument.getValue();
        assertThat(savedStudent.toReference()).isEqualTo("username");
    }
}
