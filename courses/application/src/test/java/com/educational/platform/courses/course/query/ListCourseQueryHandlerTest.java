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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListCourseQueryHandlerTest {

    @Mock
    private CourseRepository repository;

    @InjectMocks
    private ListCourseQueryHandler sut;

    @Test
    void handle_coursesExist_listReturned() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseLightDTO dto = new CourseLightDTO(uuid, "name", "description", 10);
        when(repository.list()).thenReturn(List.of(dto));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0))
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("name", "name")
                .hasFieldOrPropertyWithValue("numberOfStudents", 10);
    }

    @Test
    void handle_noCoursesExist_emptyListReturned() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        when(repository.list()).thenReturn(Collections.emptyList());

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).isEmpty();
    }
}
