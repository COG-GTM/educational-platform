package com.educational.platform.courses.course.query;

import com.educational.platform.courses.course.CourseLightDTO;
import com.educational.platform.courses.course.CourseRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListCourseQueryHandlerTest {

    @Mock
    private CourseRepository repository;

    @InjectMocks
    private ListCourseQueryHandler sut;

    @Test
    void handle_coursesExist_returnsRepositoryListing() {
        // given - the listing read path returns the repository's light projections unchanged
        final CourseLightDTO dto = new CourseLightDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), "Java", "Java course", 0);
        when(repository.list()).thenReturn(List.of(dto));

        // when
        final List<CourseLightDTO> result = sut.handle(new ListCourseQuery());

        // then
        assertThat(result).containsExactly(dto);
        verify(repository).list();
    }

    @Test
    void handle_noCourses_returnsEmptyList() {
        // given - an empty catalogue yields an empty list rather than null
        when(repository.list()).thenReturn(Collections.emptyList());

        // when
        final List<CourseLightDTO> result = sut.handle(new ListCourseQuery());

        // then
        assertThat(result).isEmpty();
    }
}
