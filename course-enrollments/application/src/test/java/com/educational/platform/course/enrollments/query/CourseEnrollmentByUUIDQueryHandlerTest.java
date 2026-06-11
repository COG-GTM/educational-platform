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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseEnrollmentByUUIDQueryHandlerTest {

    @Mock
    private CourseEnrollmentRepository repository;

    private CourseEnrollmentByUUIDQueryHandler sut;

    @BeforeEach
    void setUp() {
        sut = new CourseEnrollmentByUUIDQueryHandler(repository);
        UserDetails userDetails = new User("student", "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "password", Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handle_existingEnrollment_returnsCourseEnrollmentDTO() {
        // given
        final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseEnrollmentByUUIDQuery query = new CourseEnrollmentByUUIDQuery(enrollmentUuid);
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(enrollmentUuid, courseUuid, "student", CompletionStatusDTO.IN_PROGRESS);
        when(repository.query(enrollmentUuid, "student")).thenReturn(Optional.of(dto));

        // when
        final Optional<CourseEnrollmentDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().uuid()).isEqualTo(enrollmentUuid);
        assertThat(result.get().student()).isEqualTo("student");
    }

    @Test
    void handle_existingEnrollment_verifyAllDTOFields() {
        // given
        final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseEnrollmentByUUIDQuery query = new CourseEnrollmentByUUIDQuery(enrollmentUuid);
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(enrollmentUuid, courseUuid, "student", CompletionStatusDTO.COMPLETED);
        when(repository.query(enrollmentUuid, "student")).thenReturn(Optional.of(dto));

        // when
        final Optional<CourseEnrollmentDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().course()).isEqualTo(courseUuid);
        assertThat(result.get().completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
    }

    @Test
    void handle_nonExistingEnrollment_returnsEmpty() {
        // given
        final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseEnrollmentByUUIDQuery query = new CourseEnrollmentByUUIDQuery(enrollmentUuid);
        when(repository.query(enrollmentUuid, "student")).thenReturn(Optional.empty());

        // when
        final Optional<CourseEnrollmentDTO> result = sut.handle(query);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void handle_delegatesWithAuthenticatedUsername() {
        // given
        SecurityContextHolder.clearContext();
        UserDetails userDetails = new User("other-student", "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "password", Collections.emptyList()));

        final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseEnrollmentByUUIDQuery query = new CourseEnrollmentByUUIDQuery(enrollmentUuid);
        when(repository.query(enrollmentUuid, "other-student")).thenReturn(Optional.empty());

        // when
        sut.handle(query);

        // then
        verify(repository).query(enrollmentUuid, "other-student");
    }

    @Test
    void handle_noAuthentication_throwsNullPointerException() {
        // given
        SecurityContextHolder.clearContext();
        final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseEnrollmentByUUIDQuery query = new CourseEnrollmentByUUIDQuery(enrollmentUuid);

        // when / then
        assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void handle_passesExactQueryUuidToRepository() {
        // given
        final UUID enrollmentUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        final CourseEnrollmentByUUIDQuery query = new CourseEnrollmentByUUIDQuery(enrollmentUuid);
        when(repository.query(enrollmentUuid, "student")).thenReturn(Optional.empty());

        // when
        sut.handle(query);

        // then
        verify(repository).query(enrollmentUuid, "student");
    }

    @Test
    void handle_returnsSameOptionalFromRepository() {
        // given
        final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseEnrollmentByUUIDQuery query = new CourseEnrollmentByUUIDQuery(enrollmentUuid);
        final Optional<CourseEnrollmentDTO> expectedResult = Optional.of(
                new CourseEnrollmentDTO(enrollmentUuid, courseUuid, "student", CompletionStatusDTO.IN_PROGRESS));
        when(repository.query(enrollmentUuid, "student")).thenReturn(expectedResult);

        // when
        final Optional<CourseEnrollmentDTO> result = sut.handle(query);

        // then
        assertThat(result).isSameAs(expectedResult);
    }

    @Test
    void handle_principalNotUserDetails_throwsClassCastException() {
        // given
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("plain-string", "password", Collections.emptyList()));
        final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseEnrollmentByUUIDQuery query = new CourseEnrollmentByUUIDQuery(enrollmentUuid);

        // when / then
        assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(ClassCastException.class);
    }

    @Test
    void handle_nullUuidInQuery_delegatesNullToRepository() {
        // given
        final CourseEnrollmentByUUIDQuery query = new CourseEnrollmentByUUIDQuery(null);
        when(repository.query(null, "student")).thenReturn(Optional.empty());

        // when
        final Optional<CourseEnrollmentDTO> result = sut.handle(query);

        // then
        assertThat(result).isEmpty();
        verify(repository).query(null, "student");
    }

    @Test
    void handle_repositoryReturnsEmpty_neverAccessesDTOFields() {
        // given
        final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseEnrollmentByUUIDQuery query = new CourseEnrollmentByUUIDQuery(enrollmentUuid);
        when(repository.query(enrollmentUuid, "student")).thenReturn(Optional.empty());

        // when
        final Optional<CourseEnrollmentDTO> result = sut.handle(query);

        // then
        assertThat(result).isNotPresent();
    }
}
