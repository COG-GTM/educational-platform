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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseTeacherCheckerTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CourseTeacherChecker sut;

    @Test
    void hasAccess_currentUserIsTeacher_accessGranted() {
        // given - access is delegated to the repository, which checks course ownership by the authenticated username
        when(authentication.getName()).thenReturn("teacher");
        when(courseRepository.isTeacher(COURSE_ID, "teacher")).thenReturn(true);

        // when
        final boolean access = sut.hasAccess(authentication, COURSE_ID);

        // then
        assertThat(access).isTrue();
    }

    @Test
    void hasAccess_currentUserIsNotTeacher_accessDenied() {
        // given - a user who does not own the course is denied
        when(authentication.getName()).thenReturn("someoneelse");
        when(courseRepository.isTeacher(COURSE_ID, "someoneelse")).thenReturn(false);

        // when
        final boolean access = sut.hasAccess(authentication, COURSE_ID);

        // then
        assertThat(access).isFalse();
    }

    @Test
    void hasAccess_delegatesCourseIdAndAuthenticatedUsernameToRepository() {
        // given - the checker must forward the course uuid and the authenticated principal's name verbatim
        when(authentication.getName()).thenReturn("teacher");
        when(courseRepository.isTeacher(COURSE_ID, "teacher")).thenReturn(true);

        // when
        sut.hasAccess(authentication, COURSE_ID);

        // then
        verify(courseRepository).isTeacher(COURSE_ID, "teacher");
    }
}
