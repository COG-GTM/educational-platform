package com.educational.platform.course.enrollments.query;

import com.educational.platform.course.enrollments.CompletionStatusDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListCourseEnrollmentsQueryHandlerTest {

    @Mock
    private CourseEnrollmentRepository repository;

    @InjectMocks
    private ListCourseEnrollmentsQueryHandler sut;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handle_authenticatedStudent_scopesListingToPrincipalUsername() {
        // given - the listing returns only the authenticated student's enrollments, so the principal
        // username is forwarded to the repository
        authenticateAs("student");
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(
                UUID.randomUUID(), UUID.randomUUID(), "student", CompletionStatusDTO.IN_PROGRESS);
        when(repository.query("student")).thenReturn(List.of(dto));

        // when
        final List<CourseEnrollmentDTO> result = sut.handle(new ListCourseEnrollmentsQuery());

        // then
        assertThat(result).containsExactly(dto);
        verify(repository).query("student");
    }

    @Test
    void handle_studentWithoutEnrollments_returnsEmptyList() {
        // given - a student with no enrollments yields an empty listing rather than null
        authenticateAs("student");
        when(repository.query("student")).thenReturn(Collections.emptyList());

        // when
        final List<CourseEnrollmentDTO> result = sut.handle(new ListCourseEnrollmentsQuery());

        // then
        assertThat(result).isEmpty();
    }

    private void authenticateAs(String username) {
        final UserDetails principal = User.withUsername(username)
                .password("password")
                .authorities("ROLE_STUDENT")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities()));
    }
}
