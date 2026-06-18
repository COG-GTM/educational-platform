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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseEnrollmentByUUIDQueryHandlerTest {

    @Mock
    private CourseEnrollmentRepository repository;

    @InjectMocks
    private CourseEnrollmentByUUIDQueryHandler sut;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handle_authenticatedStudent_scopesQueryToPrincipalUsernameAndReturnsResult() {
        // given - the read is scoped to the authenticated student, so the principal username is combined
        // with the requested uuid before delegating to the repository
        authenticateAs("student");
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseEnrollmentDTO dto =
                new CourseEnrollmentDTO(uuid, UUID.randomUUID(), "student", CompletionStatusDTO.IN_PROGRESS);
        when(repository.query(uuid, "student")).thenReturn(Optional.of(dto));

        // when
        final Optional<CourseEnrollmentDTO> result = sut.handle(new CourseEnrollmentByUUIDQuery(uuid));

        // then
        assertThat(result).containsSame(dto);
        verify(repository).query(uuid, "student");
    }

    @Test
    void handle_noEnrollmentForStudent_returnsEmpty() {
        // given - an enrollment that does not belong to the authenticated student resolves to no result
        authenticateAs("student");
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        when(repository.query(uuid, "student")).thenReturn(Optional.empty());

        // when
        final Optional<CourseEnrollmentDTO> result = sut.handle(new CourseEnrollmentByUUIDQuery(uuid));

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
