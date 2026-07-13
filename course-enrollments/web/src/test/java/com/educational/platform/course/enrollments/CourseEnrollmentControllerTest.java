package com.educational.platform.course.enrollments;

import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQuery;
import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQueryHandler;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommand;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommandHandler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    void enroll_validCourse_delegatesToHandlerAndReturnsUuid() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID enrollmentId = UUID.fromString("123e4567-e89b-12d3-a456-426655440009");
        when(registerStudentToCourseCommandHandler.handle(new RegisterStudentToCourseCommand(courseId)))
                .thenReturn(enrollmentId);

        // when
        final UUID result = sut.enroll(courseId, new CourseEnrollmentRequest("username"));

        // then
        assertThat(result).isEqualTo(enrollmentId);
        final ArgumentCaptor<RegisterStudentToCourseCommand> argument =
                ArgumentCaptor.forClass(RegisterStudentToCourseCommand.class);
        verify(registerStudentToCourseCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().courseId()).isEqualTo(courseId);
    }

    @Test
    void courseEnrollments_existingEnrollments_delegatesToHandlerAndReturnsDtos() {
        // given
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440009"),
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"),
                "username",
                CompletionStatusDTO.IN_PROGRESS);
        when(listCourseEnrollmentsQueryHandler.handle(new ListCourseEnrollmentsQuery()))
                .thenReturn(List.of(dto));

        // when
        final List<CourseEnrollmentDTO> result = sut.courseEnrollments();

        // then
        assertThat(result).containsExactly(dto);
        verify(listCourseEnrollmentsQueryHandler).handle(new ListCourseEnrollmentsQuery());
    }
}
