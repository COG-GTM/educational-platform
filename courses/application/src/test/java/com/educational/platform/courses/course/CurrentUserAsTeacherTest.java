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
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CurrentUserAsTeacherTest {

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private CurrentUserAsTeacher sut;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userAsTeacher_authenticatedUser_resolvesPrincipalUsernameAndReturnsRepositoryTeacher() {
        // given - the resolver reads the username off the authenticated principal and looks the teacher
        // projection up by it; the command handlers rely on this seam to attach the owning teacher
        authenticateAs("teacher");
        final Teacher teacher = new Teacher(new CreateTeacherCommand("teacher"));
        when(teacherRepository.findByUsername("teacher")).thenReturn(teacher);

        // when
        final Teacher result = sut.userAsTeacher();

        // then
        assertThat(result).isSameAs(teacher);
        verify(teacherRepository).findByUsername("teacher");
    }

    @Test
    void userAsTeacher_noMatchingTeacher_returnsNull() {
        // given - the resolver performs no guard, so a username with no teacher projection is forwarded as null
        authenticateAs("teacher");
        when(teacherRepository.findByUsername("teacher")).thenReturn(null);

        // when
        final Teacher result = sut.userAsTeacher();

        // then
        assertThat(result).isNull();
    }

    private void authenticateAs(String username) {
        final UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("password")
                .authorities("ROLE_TEACHER")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities()));
    }
}
