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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseEnrollmentQueryHandlersTest {

    @Mock
    private CourseEnrollmentRepository repository;

    private CourseEnrollmentByUUIDQueryHandler byUUIDQueryHandler;
    private ListCourseEnrollmentsQueryHandler listQueryHandler;

    @BeforeEach
    void setUp() {
        byUUIDQueryHandler = new CourseEnrollmentByUUIDQueryHandler(repository);
        listQueryHandler = new ListCourseEnrollmentsQueryHandler(repository);

        final UserDetails principal = User.withUsername("student")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, "password", Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void byUUID_existingEnrollment_returnsEnrollmentForCurrentUser() {
        // given
        final UUID uuid = UUID.randomUUID();
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(uuid, UUID.randomUUID(), "student", CompletionStatusDTO.IN_PROGRESS);
        when(repository.query(uuid, "student")).thenReturn(Optional.of(dto));

        // when
        final Optional<CourseEnrollmentDTO> result = byUUIDQueryHandler.handle(new CourseEnrollmentByUUIDQuery(uuid));

        // then
        assertThat(result).contains(dto);
    }

    @Test
    void list_returnsEnrollmentsForCurrentUser() {
        // given
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(UUID.randomUUID(), UUID.randomUUID(), "student", CompletionStatusDTO.COMPLETED);
        when(repository.query("student")).thenReturn(List.of(dto));

        // when
        final List<CourseEnrollmentDTO> result = listQueryHandler.handle(new ListCourseEnrollmentsQuery());

        // then
        assertThat(result).containsExactly(dto);
    }

    @Test
    void list_noEnrollments_returnsEmptyList() {
        // given
        when(repository.query("student")).thenReturn(List.of());

        // when
        final List<CourseEnrollmentDTO> result = listQueryHandler.handle(new ListCourseEnrollmentsQuery());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void byUUID_enrollmentNotFound_returnsEmpty() {
        // given
        final UUID uuid = UUID.randomUUID();
        when(repository.query(uuid, "student")).thenReturn(Optional.empty());

        // when
        final Optional<CourseEnrollmentDTO> result = byUUIDQueryHandler.handle(new CourseEnrollmentByUUIDQuery(uuid));

        // then
        assertThat(result).isEmpty();
    }
}
