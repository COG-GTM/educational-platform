package com.educational.platform.courses.course.query;

import com.educational.platform.courses.course.CourseDTO;
import com.educational.platform.courses.course.CourseLightDTO;
import com.educational.platform.courses.course.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseQueryHandlersTest {

    @Mock
    private CourseRepository repository;

    private CourseByUUIDQueryHandler byUUIDQueryHandler;
    private ListCourseQueryHandler listCourseQueryHandler;

    @BeforeEach
    void setUp() {
        byUUIDQueryHandler = new CourseByUUIDQueryHandler(repository);
        listCourseQueryHandler = new ListCourseQueryHandler(repository);
    }

    @Test
    void courseByUUID_existing_returnsCourse() {
        // given
        final UUID uuid = UUID.randomUUID();
        final CourseDTO dto = new CourseDTO(uuid, "name", "description", 5, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(dto));

        // when
        final Optional<CourseDTO> result = byUUIDQueryHandler.handle(new CourseByUUIDQuery(uuid));

        // then
        assertThat(result).contains(dto);
    }

    @Test
    void courseByUUID_missing_returnsEmpty() {
        // given
        final UUID uuid = UUID.randomUUID();
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final Optional<CourseDTO> result = byUUIDQueryHandler.handle(new CourseByUUIDQuery(uuid));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void listCourses_returnsCoursesFromRepository() {
        // given
        final CourseLightDTO dto = new CourseLightDTO(UUID.randomUUID(), "name", "description", 3);
        when(repository.list()).thenReturn(List.of(dto));

        // when
        final List<CourseLightDTO> result = listCourseQueryHandler.handle(new ListCourseQuery());

        // then
        assertThat(result).containsExactly(dto);
    }
}
