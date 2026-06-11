package com.educational.platform.course.enrollments;

import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.StudentRepository;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private CurrentUserAsStudent sut;

    @BeforeEach
    void setUp() {
        sut = new CurrentUserAsStudent(studentRepository);

        final UserDetails principal = User.withUsername("username")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, "password", Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userAsStudent_returnsStudentForCurrentUsername() {
        // given
        final Student student = new Student(new CreateStudentCommand("username"));
        when(studentRepository.findByUsername("username")).thenReturn(student);

        // when
        final Student result = sut.userAsStudent();

        // then
        assertThat(result).isSameAs(student);
    }
}
