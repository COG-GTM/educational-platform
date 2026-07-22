package com.educational.platform.course.enrollments.query;

import com.educational.platform.course.enrollments.CompletionStatus;
import com.educational.platform.course.enrollments.CourseEnrollmentDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListCourseEnrollmentsQueryHandlerTest {

    @Mock
    private CourseEnrollmentRepository repository;

    @InjectMocks
    private ListCourseEnrollmentsQueryHandler sut;

    @BeforeEach
    void setUp() {
        final UserDetails userDetails = User.withUsername("username")
                .password("password")
                .authorities("ROLE_STUDENT")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handle_query_courseEnrollmentsOfCurrentUserReturned() {
        // given
        final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery();
        final CourseEnrollmentDTO enrollment = new CourseEnrollmentDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"),
                UUID.fromString("123e4567-e89b-12d3-a456-426655440002"),
                "username",
                CompletionStatus.IN_PROGRESS);
        when(repository.query("username")).thenReturn(List.of(enrollment));

        // when
        final List<CourseEnrollmentDTO> result = sut.handle(query);

        // then
        assertThat(result).containsExactly(enrollment);
    }
}
