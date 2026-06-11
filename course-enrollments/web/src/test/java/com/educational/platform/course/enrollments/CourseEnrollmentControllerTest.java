package com.educational.platform.course.enrollments;

import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQuery;
import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQueryHandler;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommand;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommandHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private RegisterStudentToCourseCommandHandler registerHandler;

    @Mock
    private ListCourseEnrollmentsQueryHandler listHandler;

    private CourseEnrollmentController sut;

    @BeforeEach
    void setUp() {
        sut = new CourseEnrollmentController(registerHandler, listHandler);
    }

    @Test
    void enroll_delegatesToRegisterHandlerWithCorrectCourseUuid() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID expectedEnrollmentUuid = UUID.randomUUID();
        when(registerHandler.handle(any(RegisterStudentToCourseCommand.class))).thenReturn(expectedEnrollmentUuid);

        // when
        final UUID result = sut.enroll(courseUuid, new CourseEnrollmentRequest("student"));

        // then
        ArgumentCaptor<RegisterStudentToCourseCommand> captor = ArgumentCaptor.forClass(RegisterStudentToCourseCommand.class);
        verify(registerHandler).handle(captor.capture());
        assertThat(captor.getValue().courseId()).isEqualTo(courseUuid);
        assertThat(result).isEqualTo(expectedEnrollmentUuid);
    }

    @Test
    void enroll_returnsUuidFromHandler() {
        // given
        final UUID courseUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440099");
        when(registerHandler.handle(any(RegisterStudentToCourseCommand.class))).thenReturn(enrollmentUuid);

        // when
        final UUID result = sut.enroll(courseUuid, new CourseEnrollmentRequest("student"));

        // then
        assertThat(result).isEqualTo(enrollmentUuid);
    }

    @Test
    void courseEnrollments_delegatesToListHandler() {
        // given
        final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(enrollmentUuid, courseUuid, "student", CompletionStatusDTO.IN_PROGRESS);
        when(listHandler.handle(any(ListCourseEnrollmentsQuery.class))).thenReturn(List.of(dto));

        // when
        final List<CourseEnrollmentDTO> result = sut.courseEnrollments();

        // then
        verify(listHandler).handle(any(ListCourseEnrollmentsQuery.class));
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(dto);
    }

    @Test
    void courseEnrollments_noEnrollments_returnsEmptyList() {
        // given
        when(listHandler.handle(any(ListCourseEnrollmentsQuery.class))).thenReturn(List.of());

        // when
        final List<CourseEnrollmentDTO> result = sut.courseEnrollments();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void enroll_requestBodyIgnored_onlyPathUuidUsedAsCommand() {
        // given
        final UUID pathUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID enrollmentUuid = UUID.randomUUID();
        when(registerHandler.handle(any(RegisterStudentToCourseCommand.class))).thenReturn(enrollmentUuid);

        // when — request body has different student name, but it's irrelevant to the command
        sut.enroll(pathUuid, new CourseEnrollmentRequest("another-student"));

        // then — command uses the path UUID, not the request body student
        ArgumentCaptor<RegisterStudentToCourseCommand> captor = ArgumentCaptor.forClass(RegisterStudentToCourseCommand.class);
        verify(registerHandler).handle(captor.capture());
        assertThat(captor.getValue().courseId()).isEqualTo(pathUuid);
    }
}
