package com.educational.platform.courses.course.query;

import com.educational.platform.courses.course.CourseLightDTO;
import com.educational.platform.courses.course.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListCourseQueryHandlerEmptyTest {

    @Mock
    private CourseRepository repository;

    private ListCourseQueryHandler sut;

    @BeforeEach
    void setUp() {
        sut = new ListCourseQueryHandler(repository);
    }

    @Test
    void handle_noCourses_returnsEmptyList() {
        // given
        when(repository.list()).thenReturn(List.of());

        // when
        final List<CourseLightDTO> result = sut.handle(new ListCourseQuery());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void handle_multipleCourses_returnsAll() {
        // given
        final CourseLightDTO dto1 = new CourseLightDTO(java.util.UUID.randomUUID(), "Course 1", "Desc 1", 5);
        final CourseLightDTO dto2 = new CourseLightDTO(java.util.UUID.randomUUID(), "Course 2", "Desc 2", 10);
        when(repository.list()).thenReturn(List.of(dto1, dto2));

        // when
        final List<CourseLightDTO> result = sut.handle(new ListCourseQuery());

        // then
        assertThat(result).containsExactly(dto1, dto2);
    }
}
