package com.educational.platform.course.enrollments;

import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQuery;
import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQueryHandler;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommand;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommandHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseEnrollmentControllerReturnValueTest {

    @Mock
    private RegisterStudentToCourseCommandHandler registerStudentToCourseCommandHandler;

    @Mock
    private ListCourseEnrollmentsQueryHandler listCourseEnrollmentsQueryHandler;

    @InjectMocks
    private CourseEnrollmentController sut;

    @Test
    void enroll_handlerReturnsUuid_controllerReturnsExactSameUuid() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440099");
        final CourseEnrollmentRequest request = new CourseEnrollmentRequest("student");
        when(registerStudentToCourseCommandHandler.handle(any(RegisterStudentToCourseCommand.class)))
                .thenReturn(enrollmentUuid);

        // when
        final UUID result = sut.enroll(courseUuid, request);

        // then
        assertThat(result).isEqualTo(enrollmentUuid);
    }

    @Test
    void courseEnrollments_multipleEnrollments_allReturned() {
        // given
        final CourseEnrollmentDTO dto1 = new CourseEnrollmentDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"),
                UUID.fromString("123e4567-e89b-12d3-a456-426655440010"),
                "alice",
                CompletionStatusDTO.IN_PROGRESS
        );
        final CourseEnrollmentDTO dto2 = new CourseEnrollmentDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440002"),
                UUID.fromString("123e4567-e89b-12d3-a456-426655440020"),
                "bob",
                CompletionStatusDTO.COMPLETED
        );
        when(listCourseEnrollmentsQueryHandler.handle(any(ListCourseEnrollmentsQuery.class)))
                .thenReturn(List.of(dto1, dto2));

        // when
        final List<CourseEnrollmentDTO> result = sut.courseEnrollments();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).student()).isEqualTo("alice");
        assertThat(result.get(1).student()).isEqualTo("bob");
        assertThat(result.get(1).completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
    }
}
