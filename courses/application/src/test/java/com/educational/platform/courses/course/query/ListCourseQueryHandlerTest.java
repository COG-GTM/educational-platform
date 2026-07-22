package com.educational.platform.courses.course.query;

import com.educational.platform.courses.course.CourseLightDTO;
import com.educational.platform.courses.course.CourseRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListCourseQueryHandlerTest {

    @Mock
    private CourseRepository repository;

    @InjectMocks
    private ListCourseQueryHandler sut;

    @Test
    void handle_query_coursesReturned() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO course = new CourseLightDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), "name", "description", 5);
        when(repository.list()).thenReturn(List.of(course));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).containsExactly(course);
    }
}
