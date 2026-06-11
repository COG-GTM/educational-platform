package com.educational.platform.course.enrollments.query;

import com.educational.platform.course.enrollments.CompletionStatusDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;

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
}
