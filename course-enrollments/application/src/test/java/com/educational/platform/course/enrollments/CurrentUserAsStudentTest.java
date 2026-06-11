package com.educational.platform.course.enrollments;

import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.StudentRepository;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CurrentUserAsStudentTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private CurrentUserAsStudent sut;

    @Test
    void userAsStudent_authenticatedUser_returnsStudent() {
        // given
        final UserDetails userDetails = new User("student", "password", Collections.emptyList());
        final var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        final Student student = new Student(new CreateStudentCommand("student"));
        when(studentRepository.findByUsername("student")).thenReturn(student);

        // when
        final Student result = sut.userAsStudent();

        // then
        assertThat(result).isEqualTo(student);
        assertThat(result.toReference()).isEqualTo("student");

        SecurityContextHolder.clearContext();
    }
}
