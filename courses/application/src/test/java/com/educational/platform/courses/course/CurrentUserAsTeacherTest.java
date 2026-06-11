package com.educational.platform.courses.course;

import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.TeacherRepository;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
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
public class CurrentUserAsTeacherTest {

    @Mock
    private TeacherRepository teacherRepository;

    private CurrentUserAsTeacher sut;

    @BeforeEach
    void setUp() {
        sut = new CurrentUserAsTeacher(teacherRepository);

        final UserDetails principal = User.withUsername("teacher-user")
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
    void userAsTeacher_returnsTeacherForCurrentUsername() {
        // given
        final Teacher teacher = new Teacher(new CreateTeacherCommand("teacher-user"));
        when(teacherRepository.findByUsername("teacher-user")).thenReturn(teacher);

        // when
        final Teacher result = sut.userAsTeacher();

        // then
        assertThat(result).isSameAs(teacher);
    }
}
