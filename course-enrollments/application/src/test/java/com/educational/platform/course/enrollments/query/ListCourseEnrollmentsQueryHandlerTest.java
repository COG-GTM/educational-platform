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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListCourseEnrollmentsQueryHandlerTest {

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
    void handle_currentStudentHasEnrollments_dtosReturned() {
        // given
        authenticateAsStudent();
        final ListCourseEnrollmentsQueryHandler sut = new ListCourseEnrollmentsQueryHandler(repository);
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(ENROLLMENT, COURSE, STUDENT, CompletionStatusDTO.IN_PROGRESS);
        when(repository.query(STUDENT)).thenReturn(List.of(dto));

        // when
        final List<CourseEnrollmentDTO> result = sut.handle(new ListCourseEnrollmentsQuery());

        // then
        assertThat(result).containsExactly(dto);
    }

    private void authenticateAsStudent() {
        final User principal = new User(STUDENT, "password", List.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, "password", List.of()));
    }
}
