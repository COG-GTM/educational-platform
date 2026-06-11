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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseEnrollmentByUUIDQueryHandlerTest {

    @Mock
    private CourseEnrollmentRepository repository;

    private CourseEnrollmentByUUIDQueryHandler sut;

    @BeforeEach
    void setUp() {
        sut = new CourseEnrollmentByUUIDQueryHandler(repository);

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
    void handle_existingEnrollment_returnsDTO() {
        // given
        final UUID uuid = UUID.randomUUID();
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(uuid, UUID.randomUUID(), "student", CompletionStatusDTO.IN_PROGRESS);
        when(repository.query(uuid, "student")).thenReturn(Optional.of(dto));

        // when
        final Optional<CourseEnrollmentDTO> result = sut.handle(new CourseEnrollmentByUUIDQuery(uuid));

        // then
        assertThat(result).isPresent().contains(dto);
    }

    @Test
    void handle_nonExistentEnrollment_returnsEmpty() {
        // given
        final UUID uuid = UUID.randomUUID();
        when(repository.query(uuid, "student")).thenReturn(Optional.empty());

        // when
        final Optional<CourseEnrollmentDTO> result = sut.handle(new CourseEnrollmentByUUIDQuery(uuid));

        // then
        assertThat(result).isEmpty();
    }
}
