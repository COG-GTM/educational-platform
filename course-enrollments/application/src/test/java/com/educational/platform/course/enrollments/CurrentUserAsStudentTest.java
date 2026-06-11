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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CurrentUserAsStudentTest {

    @Mock
    private StudentRepository studentRepository;

    private CurrentUserAsStudent sut;

    @BeforeEach
    void setUp() {
        sut = new CurrentUserAsStudent(studentRepository);
        UserDetails userDetails = new User("student-user", "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "password", Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userAsStudent_authenticatedUser_returnsStudentFromRepository() {
        // given
        final Student expectedStudent = new Student(new CreateStudentCommand("student-user"));
        when(studentRepository.findByUsername("student-user")).thenReturn(expectedStudent);

        // when
        final Student result = sut.userAsStudent();

        // then
        assertThat(result).isEqualTo(expectedStudent);
        verify(studentRepository).findByUsername("student-user");
    }

    @Test
    void userAsStudent_studentNotFoundInRepository_returnsNull() {
        // given
        when(studentRepository.findByUsername("student-user")).thenReturn(null);

        // when
        final Student result = sut.userAsStudent();

        // then
        assertThat(result).isNull();
    }

    @Test
    void userAsStudent_noAuthentication_throwsNullPointerException() {
        // given
        SecurityContextHolder.clearContext();

        // when / then
        assertThatThrownBy(() -> sut.userAsStudent())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void userAsStudent_principalNotUserDetails_throwsClassCastException() {
        // given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("plain-string-principal", "password", Collections.emptyList()));

        // when / then
        assertThatThrownBy(() -> sut.userAsStudent())
                .isInstanceOf(ClassCastException.class);
    }

    @Test
    void userAsStudent_authenticatedUser_delegatesWithCorrectUsername() {
        // given
        when(studentRepository.findByUsername("student-user")).thenReturn(null);

        // when
        sut.userAsStudent();

        // then
        verify(studentRepository).findByUsername("student-user");
    }

    @Test
    void userAsStudent_multipleCalls_delegatesEachTime() {
        // given
        final Student expectedStudent = new Student(new CreateStudentCommand("student-user"));
        when(studentRepository.findByUsername("student-user")).thenReturn(expectedStudent);

        // when
        sut.userAsStudent();
        sut.userAsStudent();

        // then — no caching; repository is consulted each time
        org.mockito.Mockito.verify(studentRepository, org.mockito.Mockito.times(2)).findByUsername("student-user");
    }

    @Test
    void userAsStudent_differentAuthenticatedUser_delegatesWithNewUsername() {
        // given
        SecurityContextHolder.clearContext();
        UserDetails userDetails = new User("another-user", "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "password", Collections.emptyList()));
        when(studentRepository.findByUsername("another-user")).thenReturn(null);

        // when
        final Student result = sut.userAsStudent();

        // then
        assertThat(result).isNull();
        verify(studentRepository).findByUsername("another-user");
    }
}
