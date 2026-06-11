package com.educational.platform.courses.course.publish;

import com.educational.platform.courses.course.CourseRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CourseTeacherCheckerTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseTeacherChecker sut;

    @Test
    void hasAccess_teacherOwnsTheCourse_returnsTrue() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("teacher-user");
        when(courseRepository.isTeacher(courseId, "teacher-user")).thenReturn(true);

        // when
        final boolean result = sut.hasAccess(authentication, courseId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void hasAccess_teacherDoesNotOwnTheCourse_returnsFalse() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("other-user");
        when(courseRepository.isTeacher(courseId, "other-user")).thenReturn(false);

        // when
        final boolean result = sut.hasAccess(authentication, courseId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void hasAccess_delegatesCorrectUuidToRepository() {
        // given
        final UUID courseId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");
        when(courseRepository.isTeacher(courseId, "user")).thenReturn(true);

        // when
        sut.hasAccess(authentication, courseId);

        // then
        verify(courseRepository).isTeacher(courseId, "user");
    }

    @Test
    void hasAccess_delegatesCorrectUsernameFromAuthentication() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("specific-username");
        when(courseRepository.isTeacher(courseId, "specific-username")).thenReturn(false);

        // when
        sut.hasAccess(authentication, courseId);

        // then
        verify(authentication).getName();
        verify(courseRepository).isTeacher(courseId, "specific-username");
    }

    @Test
    void hasAccess_repositoryCalledExactlyOnce() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");
        when(courseRepository.isTeacher(courseId, "user")).thenReturn(true);

        // when
        sut.hasAccess(authentication, courseId);

        // then
        verify(courseRepository, times(1)).isTeacher(any(), any());
        verifyNoMoreInteractions(courseRepository);
    }

    @Test
    void hasAccess_nullAuthentication_throwsNullPointerException() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when / then
        assertThatThrownBy(() -> sut.hasAccess(null, courseId))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void hasAccess_nullCourseId_delegatesToRepository() {
        // given
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");
        when(courseRepository.isTeacher(null, "user")).thenReturn(false);

        // when
        final boolean result = sut.hasAccess(authentication, null);

        // then
        assertThat(result).isFalse();
        verify(courseRepository).isTeacher(null, "user");
    }

    @Test
    void hasAccess_repositoryThrowsRuntimeException_exceptionPropagated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");
        when(courseRepository.isTeacher(courseId, "user")).thenThrow(new RuntimeException("DB error"));

        // when / then
        assertThatThrownBy(() -> sut.hasAccess(authentication, courseId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB error");
    }

    @Test
    void hasAccess_repositoryThrowsIllegalArgumentException_exceptionPropagated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");
        when(courseRepository.isTeacher(courseId, "user")).thenThrow(new IllegalArgumentException("invalid"));

        // when / then
        assertThatThrownBy(() -> sut.hasAccess(authentication, courseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid");
    }

    @Test
    void hasAccess_multipleCallsWithDifferentUuids_eachDelegatedCorrectly() {
        // given
        final UUID courseId1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseId2 = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");
        when(courseRepository.isTeacher(courseId1, "user")).thenReturn(true);
        when(courseRepository.isTeacher(courseId2, "user")).thenReturn(false);

        // when
        final boolean result1 = sut.hasAccess(authentication, courseId1);
        final boolean result2 = sut.hasAccess(authentication, courseId2);

        // then
        assertThat(result1).isTrue();
        assertThat(result2).isFalse();
        verify(courseRepository).isTeacher(courseId1, "user");
        verify(courseRepository).isTeacher(courseId2, "user");
    }

    @Test
    void hasAccess_multipleCallsWithDifferentAuthentications_eachUsernamePassedCorrectly() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Authentication auth1 = mock(Authentication.class);
        final Authentication auth2 = mock(Authentication.class);
        when(auth1.getName()).thenReturn("teacher-a");
        when(auth2.getName()).thenReturn("teacher-b");
        when(courseRepository.isTeacher(courseId, "teacher-a")).thenReturn(true);
        when(courseRepository.isTeacher(courseId, "teacher-b")).thenReturn(false);

        // when
        final boolean result1 = sut.hasAccess(auth1, courseId);
        final boolean result2 = sut.hasAccess(auth2, courseId);

        // then
        assertThat(result1).isTrue();
        assertThat(result2).isFalse();
    }

    @Test
    void hasAccess_emptyUsername_delegatedToRepository() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("");
        when(courseRepository.isTeacher(courseId, "")).thenReturn(false);

        // when
        final boolean result = sut.hasAccess(authentication, courseId);

        // then
        assertThat(result).isFalse();
        verify(courseRepository).isTeacher(courseId, "");
    }

    @Test
    void hasAccess_usernameWithSpecialCharacters_delegatedToRepository() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user@domain.com");
        when(courseRepository.isTeacher(courseId, "user@domain.com")).thenReturn(true);

        // when
        final boolean result = sut.hasAccess(authentication, courseId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void hasAccess_nullUsername_delegatedToRepository() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(null);
        when(courseRepository.isTeacher(courseId, null)).thenReturn(false);

        // when
        final boolean result = sut.hasAccess(authentication, courseId);

        // then
        assertThat(result).isFalse();
        verify(courseRepository).isTeacher(courseId, null);
    }

    @Test
    void hasAccess_handlerIsStateless_sameCallReturnsFreshRepositoryResult() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");
        when(courseRepository.isTeacher(courseId, "user")).thenReturn(true, false);

        // when
        final boolean result1 = sut.hasAccess(authentication, courseId);
        final boolean result2 = sut.hasAccess(authentication, courseId);

        // then
        assertThat(result1).isTrue();
        assertThat(result2).isFalse();
    }

    @Test
    void hasAccess_uuidReferencePassedDirectlyToRepository() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");
        when(courseRepository.isTeacher(same(courseId), eq("user"))).thenReturn(true);

        // when
        sut.hasAccess(authentication, courseId);

        // then
        verify(courseRepository).isTeacher(same(courseId), eq("user"));
    }

    @Test
    void hasAccess_repositoryThrowsStackOverflowError_errorPropagated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");
        when(courseRepository.isTeacher(courseId, "user")).thenThrow(new StackOverflowError());

        // when / then
        assertThatThrownBy(() -> sut.hasAccess(authentication, courseId))
                .isInstanceOf(StackOverflowError.class);
    }

    @Test
    void hasAccess_afterExceptionNextCallStillDelegatesToRepository() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");
        when(courseRepository.isTeacher(courseId, "user"))
                .thenThrow(new RuntimeException("first call fails"))
                .thenReturn(true);

        // when
        try { sut.hasAccess(authentication, courseId); } catch (RuntimeException ignored) {}
        final boolean result = sut.hasAccess(authentication, courseId);

        // then
        assertThat(result).isTrue();
        verify(courseRepository, times(2)).isTeacher(courseId, "user");
    }

    @Test
    void classAnnotatedWithComponent() {
        // then
        assertThat(CourseTeacherChecker.class.isAnnotationPresent(Component.class)).isTrue();
    }

    @Test
    void constructorAcceptsSingleCourseRepositoryParameter() throws NoSuchMethodException {
        // when
        final Constructor<?> constructor = CourseTeacherChecker.class.getDeclaredConstructor(CourseRepository.class);

        // then
        assertThat(constructor).isNotNull();
        assertThat(constructor.getParameterCount()).isEqualTo(1);
        assertThat(constructor.getParameterTypes()[0]).isEqualTo(CourseRepository.class);
    }

    @Test
    void courseRepositoryFieldIsPrivateAndFinal() throws NoSuchFieldException {
        // when
        final Field field = CourseTeacherChecker.class.getDeclaredField("courseRepository");

        // then
        assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
    }

    @Test
    void hasAccessMethodIsPublic() throws NoSuchMethodException {
        // when
        final var method = CourseTeacherChecker.class.getDeclaredMethod("hasAccess", Authentication.class, UUID.class);

        // then
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void hasAccessMethodReturnTypeIsBoolean() throws NoSuchMethodException {
        // when
        final var method = CourseTeacherChecker.class.getDeclaredMethod("hasAccess", Authentication.class, UUID.class);

        // then
        assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    void hasAccessMethodParameterTypes() throws NoSuchMethodException {
        // when
        final var method = CourseTeacherChecker.class.getDeclaredMethod("hasAccess", Authentication.class, UUID.class);

        // then
        assertThat(method.getParameterCount()).isEqualTo(2);
        assertThat(method.getParameterTypes()[0]).isEqualTo(Authentication.class);
        assertThat(method.getParameterTypes()[1]).isEqualTo(UUID.class);
    }
}
