package com.educational.platform.course.enrollments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.StudentRepository;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

public class CurrentUserAsStudentTest {

    private final StudentRepository studentRepository = mock(StudentRepository.class);

    private CurrentUserAsStudent sut;

    @BeforeEach
    void setUp() {
        sut = new CurrentUserAsStudent(studentRepository);
        final UserDetails principal = User.withUsername("student").password("secret").roles("STUDENT").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void username_authenticatedUser_principalUsername() {
        // when
        final String username = sut.username();

        // then
        assertThat(username).isEqualTo("student");
    }

    @Test
    void userAsStudent_replicatedStudent_lookedUpByCurrentUsername() {
        // given
        final Student student = new Student(new CreateStudentCommand("student"));
        when(studentRepository.findByUsername("student")).thenReturn(student);

        // when
        final Student result = sut.userAsStudent();

        // then
        assertThat(result).isSameAs(student);
        verify(studentRepository).findByUsername("student");
    }

    @Test
    void userAsStudent_studentNotReplicatedYet_null() {
        // given
        when(studentRepository.findByUsername(anyString())).thenReturn(null);

        // when
        final Student result = sut.userAsStudent();

        // then
        assertThat(result).isNull();
        verify(studentRepository).findByUsername("student");
    }
}
