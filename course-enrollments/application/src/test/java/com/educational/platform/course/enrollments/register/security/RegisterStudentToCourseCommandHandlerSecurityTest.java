package com.educational.platform.course.enrollments.register.security;

import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommand;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommandHandler;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Sql(scripts = "classpath:course.sql")
@SpringBootTest(properties = "com.educational.platform.security.enabled=true")
public class RegisterStudentToCourseCommandHandlerSecurityTest {

    private final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @MockitoSpyBean
    private RegisterStudentToCourseCommandHandler sut;

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void handle_userIsStudent_studentEnrolled() {
        // given
        var command = new RegisterStudentToCourseCommand(courseUuid);

        // when
        var result = sut.handle(command);

        // then
        final Optional<CourseEnrollment> saved = courseEnrollmentRepository.findByUuid(result);
        assertThat(saved).isNotEmpty();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void handle_userIsTeacher_accessDeniedException() {
        // given
        var command = new RegisterStudentToCourseCommand(courseUuid);

        // when
        final ThrowingCallable registerAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(registerAction)
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = {})
    void handle_userHasNoRoles_accessDeniedException() {
        // given
        var command = new RegisterStudentToCourseCommand(courseUuid);

        // when
        final ThrowingCallable registerAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(registerAction)
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void handle_userIsAdmin_accessDeniedException() {
        // given
        var command = new RegisterStudentToCourseCommand(courseUuid);

        // when
        final ThrowingCallable registerAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(registerAction)
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "student", roles = {"STUDENT", "TEACHER"})
    void handle_userHasMultipleRolesIncludingStudent_accessAllowed() {
        // given
        var command = new RegisterStudentToCourseCommand(courseUuid);

        // when
        var result = sut.handle(command);

        // then — user with STUDENT + TEACHER roles passes hasRole('STUDENT') check
        final Optional<CourseEnrollment> saved = courseEnrollmentRepository.findByUuid(result);
        assertThat(saved).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void handle_validCommand_enrollmentHasInProgressStatus() {
        // given
        var command = new RegisterStudentToCourseCommand(courseUuid);

        // when
        var result = sut.handle(command);

        // then — newly created enrollment starts with IN_PROGRESS status
        final Optional<CourseEnrollment> saved = courseEnrollmentRepository.findByUuid(result);
        assertThat(saved).isPresent();
        assertThat(saved.get()).hasFieldOrPropertyWithValue("completionStatus",
                com.educational.platform.course.enrollments.CompletionStatus.IN_PROGRESS);
    }
}
