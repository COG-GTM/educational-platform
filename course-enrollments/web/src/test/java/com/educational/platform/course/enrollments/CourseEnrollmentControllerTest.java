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
import static org.mockito.Mockito.*;
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

    @Test
    void courseEnrollments_multipleEnrollments_returnsAll() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UUID course1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final UUID course2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440004");
        final CourseEnrollmentDTO dto1 = new CourseEnrollmentDTO(uuid1, course1, "student", CompletionStatusDTO.IN_PROGRESS);
        final CourseEnrollmentDTO dto2 = new CourseEnrollmentDTO(uuid2, course2, "student", CompletionStatusDTO.COMPLETED);
        when(listHandler.handle(any(ListCourseEnrollmentsQuery.class))).thenReturn(List.of(dto1, dto2));

        // when
        final List<CourseEnrollmentDTO> result = sut.courseEnrollments();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CourseEnrollmentDTO::uuid).containsExactly(uuid1, uuid2);
    }

    @Test
    void enroll_handlerThrowsException_propagatesToCaller() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(registerHandler.handle(any(RegisterStudentToCourseCommand.class)))
                .thenThrow(new RuntimeException("enrollment failed"));

        // when / then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> sut.enroll(courseUuid, new CourseEnrollmentRequest("student")));
    }

    @Test
    void courseEnrollments_handlerThrowsException_propagatesToCaller() {
        // given
        when(listHandler.handle(any(ListCourseEnrollmentsQuery.class)))
                .thenThrow(new RuntimeException("list failed"));

        // when / then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> sut.courseEnrollments());
    }

    @Test
    void enroll_handlerReturnsNull_returnsNull() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(registerHandler.handle(any(RegisterStudentToCourseCommand.class))).thenReturn(null);

        // when
        final UUID result = sut.enroll(courseUuid, new CourseEnrollmentRequest("student"));

        // then
        assertThat(result).isNull();
    }

    @Test
    void enroll_nullRequestBody_doesNotThrow() {
        // given — request body is unused in the controller logic
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID enrollmentUuid = UUID.randomUUID();
        when(registerHandler.handle(any(RegisterStudentToCourseCommand.class))).thenReturn(enrollmentUuid);

        // when
        final UUID result = sut.enroll(courseUuid, null);

        // then
        assertThat(result).isEqualTo(enrollmentUuid);
    }

    @Test
    void courseEnrollments_handlerReturnsNull_returnsNull() {
        // given
        when(listHandler.handle(any(ListCourseEnrollmentsQuery.class))).thenReturn(null);

        // when
        final List<CourseEnrollmentDTO> result = sut.courseEnrollments();

        // then
        assertThat(result).isNull();
    }

    @Test
    void enroll_nullPathUuid_createsCommandWithNullCourseId() {
        // given
        final UUID enrollmentUuid = UUID.randomUUID();
        when(registerHandler.handle(any(RegisterStudentToCourseCommand.class))).thenReturn(enrollmentUuid);

        // when
        sut.enroll(null, new CourseEnrollmentRequest("student"));

        // then
        ArgumentCaptor<RegisterStudentToCourseCommand> captor = ArgumentCaptor.forClass(RegisterStudentToCourseCommand.class);
        verify(registerHandler).handle(captor.capture());
        assertThat(captor.getValue().courseId()).isNull();
    }

    @Test
    void courseEnrollments_returnsSameListFromHandler() {
        // given
        final List<CourseEnrollmentDTO> expectedList = List.of(
                new CourseEnrollmentDTO(UUID.randomUUID(), UUID.randomUUID(), "student", CompletionStatusDTO.IN_PROGRESS));
        when(listHandler.handle(any(ListCourseEnrollmentsQuery.class))).thenReturn(expectedList);

        // when
        final List<CourseEnrollmentDTO> result = sut.courseEnrollments();

        // then
        assertThat(result).isSameAs(expectedList);
    }

    @Test
    void enroll_doesNotInteractWithListHandler() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(registerHandler.handle(any(RegisterStudentToCourseCommand.class))).thenReturn(UUID.randomUUID());

        // when
        sut.enroll(courseUuid, new CourseEnrollmentRequest("student"));

        // then
        verifyNoInteractions(listHandler);
    }

    @Test
    void courseEnrollments_doesNotInteractWithRegisterHandler() {
        // given
        when(listHandler.handle(any(ListCourseEnrollmentsQuery.class))).thenReturn(List.of());

        // when
        sut.courseEnrollments();

        // then
        verifyNoInteractions(registerHandler);
    }

    @Test
    void enroll_differentCourseUuids_passedCorrectly() {
        // given
        final UUID courseUuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        when(registerHandler.handle(any(RegisterStudentToCourseCommand.class))).thenReturn(UUID.randomUUID());

        // when
        sut.enroll(courseUuid1, new CourseEnrollmentRequest("s1"));
        sut.enroll(courseUuid2, new CourseEnrollmentRequest("s2"));

        // then
        ArgumentCaptor<RegisterStudentToCourseCommand> captor = ArgumentCaptor.forClass(RegisterStudentToCourseCommand.class);
        verify(registerHandler, times(2)).handle(captor.capture());
        assertThat(captor.getAllValues().get(0).courseId()).isEqualTo(courseUuid1);
        assertThat(captor.getAllValues().get(1).courseId()).isEqualTo(courseUuid2);
    }

    @Test
    void courseEnrollments_passesListCourseEnrollmentsQueryToHandler() {
        // given
        when(listHandler.handle(any(ListCourseEnrollmentsQuery.class))).thenReturn(List.of());

        // when
        sut.courseEnrollments();

        // then
        ArgumentCaptor<ListCourseEnrollmentsQuery> captor = ArgumentCaptor.forClass(ListCourseEnrollmentsQuery.class);
        verify(listHandler).handle(captor.capture());
        assertThat(captor.getValue()).isNotNull();
        assertThat(captor.getValue()).isInstanceOf(ListCourseEnrollmentsQuery.class);
    }

    @Test
    void enroll_multipleCalls_eachCallCreatesNewCommand() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(registerHandler.handle(any(RegisterStudentToCourseCommand.class))).thenReturn(UUID.randomUUID());

        // when
        sut.enroll(courseUuid, new CourseEnrollmentRequest("s1"));
        sut.enroll(courseUuid, new CourseEnrollmentRequest("s2"));

        // then — each call creates a fresh command instance
        ArgumentCaptor<RegisterStudentToCourseCommand> captor = ArgumentCaptor.forClass(RegisterStudentToCourseCommand.class);
        verify(registerHandler, times(2)).handle(captor.capture());
        assertThat(captor.getAllValues().get(0).courseId()).isEqualTo(courseUuid);
        assertThat(captor.getAllValues().get(1).courseId()).isEqualTo(courseUuid);
    }
}
