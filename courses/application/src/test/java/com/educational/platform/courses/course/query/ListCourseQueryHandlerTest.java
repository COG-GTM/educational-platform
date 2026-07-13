package com.educational.platform.courses.course.query;

import java.util.List;
import java.util.UUID;

import com.educational.platform.courses.course.CourseLightDTO;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.NumberOfStudents;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        final CourseLightDTO dto = new CourseLightDTO(UUID.randomUUID(), "name", "description", new NumberOfStudents(0));
        when(repository.list()).thenReturn(List.of(dto));

        // when
        final List<CourseLightDTO> result = sut.handle(new ListCourseQuery());

        // then
        assertThat(result).containsExactly(dto);
    }
}
