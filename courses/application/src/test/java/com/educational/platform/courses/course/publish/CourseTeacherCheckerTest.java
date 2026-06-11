package com.educational.platform.courses.course.publish;

import com.educational.platform.courses.course.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseTeacherCheckerTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private Authentication authentication;

    private CourseTeacherChecker sut;

    @BeforeEach
    void setUp() {
        sut = new CourseTeacherChecker(courseRepository);
    }

    @Test
    void hasAccess_userIsTeacher_returnsTrue() {
        // given
        final UUID courseId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("teacher");
        when(courseRepository.isTeacher(courseId, "teacher")).thenReturn(true);

        // when
        final boolean result = sut.hasAccess(authentication, courseId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void hasAccess_userIsNotTeacher_returnsFalse() {
        // given
        final UUID courseId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("other-user");
        when(courseRepository.isTeacher(courseId, "other-user")).thenReturn(false);

        // when
        final boolean result = sut.hasAccess(authentication, courseId);

        // then
        assertThat(result).isFalse();
    }
}
