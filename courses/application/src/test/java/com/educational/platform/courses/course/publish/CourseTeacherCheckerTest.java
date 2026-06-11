package com.educational.platform.courses.course.publish;

import com.educational.platform.courses.course.CourseRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseTeacherCheckerTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseTeacherChecker sut;

    @Test
    void hasAccess_teacherOfCourse_true() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("teacher-username");
        when(courseRepository.isTeacher(courseId, "teacher-username")).thenReturn(true);

        // when
        final boolean result = sut.hasAccess(authentication, courseId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void hasAccess_notTeacherOfCourse_false() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("other-username");
        when(courseRepository.isTeacher(courseId, "other-username")).thenReturn(false);

        // when
        final boolean result = sut.hasAccess(authentication, courseId);

        // then
        assertThat(result).isFalse();
    }
}
