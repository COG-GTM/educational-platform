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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseEnrollmentControllerTest {

    @Mock
    private RegisterStudentToCourseCommandHandler registerStudentToCourseCommandHandler;

    @Mock
    private ListCourseEnrollmentsQueryHandler listCourseEnrollmentsQueryHandler;

    @InjectMocks
    private CourseEnrollmentController sut;

    @Test
    void enroll_validRequest_returnsUUID() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseEnrollmentRequest request = new CourseEnrollmentRequest("student");
        when(registerStudentToCourseCommandHandler.handle(any(RegisterStudentToCourseCommand.class))).thenReturn(enrollmentUuid);

        // when
        final UUID result = sut.enroll(courseUuid, request);

        // then
        assertThat(result).isEqualTo(enrollmentUuid);
        verify(registerStudentToCourseCommandHandler).handle(any(RegisterStudentToCourseCommand.class));
    }

    @Test
    void courseEnrollments_returnsListOfEnrollments() {
        // given
        final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(enrollmentUuid, courseUuid, "student", CompletionStatusDTO.IN_PROGRESS);
        when(listCourseEnrollmentsQueryHandler.handle(any(ListCourseEnrollmentsQuery.class))).thenReturn(List.of(dto));

        // when
        final List<CourseEnrollmentDTO> result = sut.courseEnrollments();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().uuid()).isEqualTo(enrollmentUuid);
        verify(listCourseEnrollmentsQueryHandler).handle(any(ListCourseEnrollmentsQuery.class));
    }
}
