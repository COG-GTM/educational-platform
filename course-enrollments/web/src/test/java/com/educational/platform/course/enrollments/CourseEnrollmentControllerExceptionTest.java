package com.educational.platform.course.enrollments;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQueryHandler;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommandHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseEnrollmentControllerExceptionTest {

    @Mock
    private RegisterStudentToCourseCommandHandler registerHandler;

    @Mock
    private ListCourseEnrollmentsQueryHandler listHandler;

    private CourseEnrollmentController sut;

    @BeforeEach
    void setUp() {
        sut = new CourseEnrollmentController(registerHandler, listHandler);
    }

    @Test
    void enroll_handlerThrowsRelatedResourceNotResolved_propagatesException() {
        // given
        final UUID courseUuid = UUID.randomUUID();
        when(registerHandler.handle(any())).thenThrow(
                new RelatedResourceIsNotResolvedException("Course cannot be found by uuid = " + courseUuid));
        final CourseEnrollmentRequest request = new CourseEnrollmentRequest("student");

        // when / then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class)
                .isThrownBy(() -> sut.enroll(courseUuid, request))
                .withMessageContaining(courseUuid.toString());
    }

    @Test
    void enroll_handlerThrowsRuntimeException_propagatesException() {
        // given
        final UUID courseUuid = UUID.randomUUID();
        when(registerHandler.handle(any())).thenThrow(new RuntimeException("Unexpected error"));
        final CourseEnrollmentRequest request = new CourseEnrollmentRequest("student");

        // when / then
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> sut.enroll(courseUuid, request))
                .withMessageContaining("Unexpected error");
    }
}
