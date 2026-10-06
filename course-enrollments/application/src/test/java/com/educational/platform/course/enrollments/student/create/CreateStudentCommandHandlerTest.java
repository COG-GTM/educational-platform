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
    void handle_newUsername_studentSaved() {
        // given
        when(studentRepository.existsByUsername("username")).thenReturn(false);

        // when
        sut.handle(new CreateStudentCommand("username"));

        // then
        final ArgumentCaptor<Student> student = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(student.capture());
        assertThat(student.getValue().toReference()).isEqualTo("username");
    }

    @Test
    void handle_alreadyRegistered_nothingSaved() {
        // given
        when(studentRepository.existsByUsername("username")).thenReturn(true);

        // when
        sut.handle(new CreateStudentCommand("username"));

        // then
        verify(studentRepository, never()).save(any());
    }
}
