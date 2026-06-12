package com.educational.platform.course.enrollments;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQuery;
import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQueryHandler;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommand;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommandHandler;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseEnrollmentControllerEdgeCaseTest {

    @Mock
    private RegisterStudentToCourseCommandHandler registerStudentToCourseCommandHandler;

    @Mock
    private ListCourseEnrollmentsQueryHandler listCourseEnrollmentsQueryHandler;

    @InjectMocks
    private CourseEnrollmentController sut;

    @Test
    void enroll_handlerThrowsRelatedResourceNotResolved_exceptionPropagated() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseEnrollmentRequest request = new CourseEnrollmentRequest("student");
        when(registerStudentToCourseCommandHandler.handle(any(RegisterStudentToCourseCommand.class)))
                .thenThrow(new RelatedResourceIsNotResolvedException("Course cannot be found by uuid = " + courseUuid));

        // when
        final ThrowableAssert.ThrowingCallable enroll = () -> sut.enroll(courseUuid, request);

        // then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class).isThrownBy(enroll);
    }

    @Test
    void enroll_handlerThrowsConstraintViolation_exceptionPropagated() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseEnrollmentRequest request = new CourseEnrollmentRequest("student");
        when(registerStudentToCourseCommandHandler.handle(any(RegisterStudentToCourseCommand.class)))
                .thenThrow(new ConstraintViolationException(Set.of()));

        // when
        final ThrowableAssert.ThrowingCallable enroll = () -> sut.enroll(courseUuid, request);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(enroll);
    }

    @Test
    void enroll_commandContainsCorrectCourseId() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID enrollmentUuid = UUID.randomUUID();
        final CourseEnrollmentRequest request = new CourseEnrollmentRequest("student");
        when(registerStudentToCourseCommandHandler.handle(any(RegisterStudentToCourseCommand.class)))
                .thenReturn(enrollmentUuid);

        // when
        sut.enroll(courseUuid, request);

        // then
        final ArgumentCaptor<RegisterStudentToCourseCommand> captor =
                ArgumentCaptor.forClass(RegisterStudentToCourseCommand.class);
        verify(registerStudentToCourseCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().courseId()).isEqualTo(courseUuid);
    }

    @Test
    void enroll_noInteractionWithQueryHandler() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseEnrollmentRequest request = new CourseEnrollmentRequest("student");
        when(registerStudentToCourseCommandHandler.handle(any(RegisterStudentToCourseCommand.class)))
                .thenReturn(UUID.randomUUID());

        // when
        sut.enroll(courseUuid, request);

        // then
        verifyNoInteractions(listCourseEnrollmentsQueryHandler);
    }

    @Test
    void courseEnrollments_noInteractionWithCommandHandler() {
        // given
        when(listCourseEnrollmentsQueryHandler.handle(any(ListCourseEnrollmentsQuery.class)))
                .thenReturn(java.util.Collections.emptyList());

        // when
        sut.courseEnrollments();

        // then
        verifyNoInteractions(registerStudentToCourseCommandHandler);
    }
}
