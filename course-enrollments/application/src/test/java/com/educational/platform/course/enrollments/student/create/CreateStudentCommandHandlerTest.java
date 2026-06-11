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
}
