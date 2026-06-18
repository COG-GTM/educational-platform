package com.educational.platform.courses.course.query;

import com.educational.platform.courses.course.CourseDTO;
import com.educational.platform.courses.course.CourseRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseByUUIDQueryHandlerTest {

    @Mock
    private CourseRepository repository;

    @InjectMocks
    private CourseByUUIDQueryHandler sut;

    @Test
    void handle_existingCourse_returnsDtoForQueriedUuid() {
        // given - the read path resolves a course projection by the query's uuid
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseDTO dto = new CourseDTO(uuid, "Java", "Java course", 0, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(dto));

        // when
        final Optional<CourseDTO> result = sut.handle(new CourseByUUIDQuery(uuid));

        // then
        assertThat(result).containsSame(dto);
        verify(repository).findDTOByUuid(uuid);
    }

    @Test
    void handle_unknownCourse_returnsEmptyOptional() {
        // given - an unknown uuid yields an empty optional rather than null, so callers can branch on absence
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final Optional<CourseDTO> result = sut.handle(new CourseByUUIDQuery(uuid));

        // then
        assertThat(result).isEmpty();
    }
}
