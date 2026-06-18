package com.educational.platform.course.enrollments;

import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.StudentRepository;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CurrentUserAsStudentTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private CurrentUserAsStudent sut;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userAsStudent_authenticatedUser_resolvesPrincipalUsernameAndReturnsRepositoryStudent() {
        // given - the enrollment flow resolves the acting student from the authenticated principal's username
        // before publishing the StudentEnrolledToCourse event
        authenticateAs("student");
        final Student student = new Student(new CreateStudentCommand("student"));
        when(studentRepository.findByUsername("student")).thenReturn(student);

        // when
        final Student result = sut.userAsStudent();

        // then
        assertThat(result).isSameAs(student);
        verify(studentRepository).findByUsername("student");
    }

    @Test
    void userAsStudent_noMatchingStudent_returnsNull() {
        // given - the resolver performs no guard, so a username with no student projection is forwarded as null
        authenticateAs("student");
        when(studentRepository.findByUsername("student")).thenReturn(null);

        // when
        final Student result = sut.userAsStudent();

        // then
        assertThat(result).isNull();
    }

    private void authenticateAs(String username) {
        final UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("password")
                .authorities("ROLE_STUDENT")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities()));
    }
}
