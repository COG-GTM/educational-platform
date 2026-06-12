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
    void userAsTeacher_authenticatedUser_teacherReturned() {
        // given
        final UserDetails userDetails = new User("teacher-username", "password", Collections.emptyList());
        final UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        final Teacher teacher = new Teacher(new CreateTeacherCommand("teacher-username"));
        when(teacherRepository.findByUsername("teacher-username")).thenReturn(teacher);

        // when
        final Teacher result = sut.userAsTeacher();

        // then
        assertThat(result)
                .hasFieldOrPropertyWithValue("username", "teacher-username");
    }
}
