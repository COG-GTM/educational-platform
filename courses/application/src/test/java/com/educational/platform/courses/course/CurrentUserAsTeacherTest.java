package com.educational.platform.courses.course;

import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.TeacherRepository;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import org.junit.jupiter.api.AfterEach;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CurrentUserAsTeacherTest {

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private CurrentUserAsTeacher sut;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userAsTeacher_authenticatedUser_returnsTeacher() {
        // given
        final UserDetails userDetails = new User("john.doe", "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "password")
        );
        final Teacher expectedTeacher = new Teacher(new CreateTeacherCommand("john.doe"));
        when(teacherRepository.findByUsername("john.doe")).thenReturn(expectedTeacher);

        // when
        final Teacher result = sut.userAsTeacher();

        // then
        assertThat(result).isSameAs(expectedTeacher);
        verify(teacherRepository).findByUsername("john.doe");
    }

    @Test
    void userAsTeacher_differentUsername_queriesCorrectUsername() {
        // given
        final UserDetails userDetails = new User("alice", "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "password")
        );
        final Teacher expectedTeacher = new Teacher(new CreateTeacherCommand("alice"));
        when(teacherRepository.findByUsername("alice")).thenReturn(expectedTeacher);

        // when
        final Teacher result = sut.userAsTeacher();

        // then
        assertThat(result.toIdentity()).isEqualTo("alice");
    }

    @Test
    void userAsTeacher_noAuthentication_throwsNullPointerException() {
        // given — no authentication set in SecurityContext
        SecurityContextHolder.clearContext();

        // when / then
        assertThatThrownBy(() -> sut.userAsTeacher())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void userAsTeacher_repositoryReturnsNull_returnsNull() {
        // given
        final UserDetails userDetails = new User("unknown", "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "password")
        );
        when(teacherRepository.findByUsername("unknown")).thenReturn(null);

        // when
        final Teacher result = sut.userAsTeacher();

        // then
        assertThat(result).isNull();
    }
}
