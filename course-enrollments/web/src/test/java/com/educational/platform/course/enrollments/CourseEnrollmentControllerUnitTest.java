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
public class CourseEnrollmentControllerUnitTest {

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
    void enroll_delegatesToRegisterHandler_returnsUuid() {
        // given
        final UUID courseUuid = UUID.randomUUID();
        final UUID enrollmentUuid = UUID.randomUUID();
        when(registerHandler.handle(any())).thenReturn(enrollmentUuid);
        final CourseEnrollmentRequest request = new CourseEnrollmentRequest("student1");

        // when
        final UUID result = sut.enroll(courseUuid, request);

        // then
        assertThat(result).isEqualTo(enrollmentUuid);
        final ArgumentCaptor<RegisterStudentToCourseCommand> captor =
                ArgumentCaptor.forClass(RegisterStudentToCourseCommand.class);
        verify(registerHandler).handle(captor.capture());
        assertThat(captor.getValue().courseId()).isEqualTo(courseUuid);
    }

    @Test
    void courseEnrollments_delegatesToListHandler_returnsDTOs() {
        // given
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(
                UUID.randomUUID(), UUID.randomUUID(), "student1", CompletionStatusDTO.IN_PROGRESS);
        when(listHandler.handle(any())).thenReturn(List.of(dto));

        // when
        final List<CourseEnrollmentDTO> result = sut.courseEnrollments();

        // then
        assertThat(result).containsExactly(dto);
        verify(listHandler).handle(any(ListCourseEnrollmentsQuery.class));
    }

    @Test
    void courseEnrollments_noEnrollments_returnsEmptyList() {
        // given
        when(listHandler.handle(any())).thenReturn(List.of());

        // when
        final List<CourseEnrollmentDTO> result = sut.courseEnrollments();

        // then
        assertThat(result).isEmpty();
    }
}
