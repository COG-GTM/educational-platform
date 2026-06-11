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
    void handle_coursesExist_listReturned() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO course1 = new CourseLightDTO(UUID.randomUUID(), "course1", "description1", 10);
        final CourseLightDTO course2 = new CourseLightDTO(UUID.randomUUID(), "course2", "description2", 20);
        when(repository.list()).thenReturn(List.of(course1, course2));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(course1, course2);
    }

    @Test
    void handle_singleCourseExists_singletonListReturned() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        final CourseLightDTO course = new CourseLightDTO(UUID.randomUUID(), "single-course", "only course", 5);
        when(repository.list()).thenReturn(List.of(course));

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(course);
    }

    @Test
    void handle_noCoursesExist_emptyListReturned() {
        // given
        final ListCourseQuery query = new ListCourseQuery();
        when(repository.list()).thenReturn(List.of());

        // when
        final List<CourseLightDTO> result = sut.handle(query);

        // then
        assertThat(result).isEmpty();
    }

}
