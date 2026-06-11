package com.educational.platform.courses.course.query;

import com.educational.platform.courses.course.CourseDTO;
import com.educational.platform.courses.course.CourseRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseByUUIDQueryHandlerTest {

    @Mock
    private CourseRepository repository;

    @InjectMocks
    private CourseByUUIDQueryHandler sut;

    @Test
    void handle_existingCourse_courseReturned() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, "name", "description", 5, new ArrayList<>());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get())
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("name", "name")
                .hasFieldOrPropertyWithValue("description", "description")
                .hasFieldOrPropertyWithValue("numberOfStudents", 5);
    }

    @Test
    void handle_nonExistingCourse_emptyOptional() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isEmpty();
    }
}
