package com.educational.platform.course.enrollments.query;

import com.educational.platform.course.enrollments.CompletionStatusDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseEnrollmentByUUIDQueryHandlerTest {

    private static final String STUDENT = "student";
    private static final UUID COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static final UUID ENROLLMENT = UUID.fromString("123e4567-e89b-12d3-a456-426655440009");

    @Mock
    private CourseEnrollmentRepository repository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handle_existingEnrollmentForCurrentStudent_dtoReturned() {
        // given
        authenticateAsStudent();
        final CourseEnrollmentByUUIDQueryHandler sut = new CourseEnrollmentByUUIDQueryHandler(repository);
        final CourseEnrollmentByUUIDQuery query = new CourseEnrollmentByUUIDQuery(ENROLLMENT);
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(ENROLLMENT, COURSE, STUDENT, CompletionStatusDTO.IN_PROGRESS);
        when(repository.query(ENROLLMENT, STUDENT)).thenReturn(Optional.of(dto));

        // when
        final Optional<CourseEnrollmentDTO> result = sut.handle(query);

        // then
        assertThat(result).contains(dto);
    }

    private void authenticateAsStudent() {
        final User principal = new User(STUDENT, "password", List.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, "password", List.of()));
    }
}
