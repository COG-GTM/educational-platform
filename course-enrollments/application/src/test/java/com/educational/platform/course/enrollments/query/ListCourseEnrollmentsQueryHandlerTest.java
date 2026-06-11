package com.educational.platform.course.enrollments.query;

import com.educational.platform.course.enrollments.CompletionStatusDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListCourseEnrollmentsQueryHandlerTest {

    @Mock
    private CourseEnrollmentRepository repository;

    private ListCourseEnrollmentsQueryHandler sut;

    @BeforeEach
    void setUp() {
        sut = new ListCourseEnrollmentsQueryHandler(repository);
        UserDetails userDetails = new User("student", "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "password", Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handle_enrollmentsExist_returnsListOfDTOs() {
        // given
        final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery();
        final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(enrollmentUuid, courseUuid, "student", CompletionStatusDTO.IN_PROGRESS);
        when(repository.query("student")).thenReturn(List.of(dto));

        // when
        final List<CourseEnrollmentDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().uuid()).isEqualTo(enrollmentUuid);
        assertThat(result.getFirst().student()).isEqualTo("student");
        assertThat(result.getFirst().completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
    }

    @Test
    void handle_multipleEnrollments_returnsAllDTOs() {
        // given
        final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery();
        final UUID enrollmentUuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UUID enrollmentUuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final UUID courseUuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440004");
        final CourseEnrollmentDTO dto1 = new CourseEnrollmentDTO(enrollmentUuid1, courseUuid1, "student", CompletionStatusDTO.IN_PROGRESS);
        final CourseEnrollmentDTO dto2 = new CourseEnrollmentDTO(enrollmentUuid2, courseUuid2, "student", CompletionStatusDTO.COMPLETED);
        when(repository.query("student")).thenReturn(List.of(dto1, dto2));

        // when
        final List<CourseEnrollmentDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CourseEnrollmentDTO::uuid).containsExactly(enrollmentUuid1, enrollmentUuid2);
        assertThat(result).extracting(CourseEnrollmentDTO::completionStatus)
                .containsExactly(CompletionStatusDTO.IN_PROGRESS, CompletionStatusDTO.COMPLETED);
    }

    @Test
    void handle_noEnrollments_returnsEmptyList() {
        // given
        final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery();
        when(repository.query("student")).thenReturn(Collections.emptyList());

        // when
        final List<CourseEnrollmentDTO> result = sut.handle(query);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void handle_delegatesWithAuthenticatedUsername() {
        // given
        SecurityContextHolder.clearContext();
        UserDetails userDetails = new User("another-student", "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "password", Collections.emptyList()));

        final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery();
        when(repository.query("another-student")).thenReturn(Collections.emptyList());

        // when
        sut.handle(query);

        // then
        verify(repository).query("another-student");
    }

    @Test
    void handle_noAuthentication_throwsNullPointerException() {
        // given
        SecurityContextHolder.clearContext();
        final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery();

        // when / then
        assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void handle_returnsSameListInstanceFromRepository() {
        // given
        final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery();
        final List<CourseEnrollmentDTO> expectedList = List.of(
                new CourseEnrollmentDTO(UUID.randomUUID(), UUID.randomUUID(), "student", CompletionStatusDTO.IN_PROGRESS));
        when(repository.query("student")).thenReturn(expectedList);

        // when
        final List<CourseEnrollmentDTO> result = sut.handle(query);

        // then
        assertThat(result).isSameAs(expectedList);
    }

    @Test
    void handle_principalNotUserDetails_throwsClassCastException() {
        // given
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("plain-string", "password", Collections.emptyList()));
        final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery();

        // when / then
        assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(ClassCastException.class);
    }

    @Test
    void handle_multipleEnrollments_preservesRepositoryOrder() {
        // given
        final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery();
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UUID uuid3 = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final CourseEnrollmentDTO dto1 = new CourseEnrollmentDTO(uuid1, UUID.randomUUID(), "student", CompletionStatusDTO.IN_PROGRESS);
        final CourseEnrollmentDTO dto2 = new CourseEnrollmentDTO(uuid2, UUID.randomUUID(), "student", CompletionStatusDTO.COMPLETED);
        final CourseEnrollmentDTO dto3 = new CourseEnrollmentDTO(uuid3, UUID.randomUUID(), "student", CompletionStatusDTO.IN_PROGRESS);
        when(repository.query("student")).thenReturn(List.of(dto1, dto2, dto3));

        // when
        final List<CourseEnrollmentDTO> result = sut.handle(query);

        // then
        assertThat(result).containsExactly(dto1, dto2, dto3);
    }

    @Test
    void handle_repositoryThrows_exceptionPropagates() {
        // given
        final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery();
        when(repository.query("student")).thenThrow(new RuntimeException("query failed"));

        // when / then
        assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("query failed");
    }
}
