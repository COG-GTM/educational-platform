package com.educational.platform.courses.course.query;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import com.educational.platform.courses.course.CourseDTO;
import com.educational.platform.courses.course.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseByUUIDQueryHandlerTest {

    @Mock
    private CourseRepository repository;

    @InjectMocks
    private CourseByUUIDQueryHandler sut;

    @Test
    void handle_courseExists_dtoReturned() {
        // given
        final UUID uuid = UUID.randomUUID();
        final CourseDTO dto = new CourseDTO(uuid, "name", "description", 0, new ArrayList<>());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(dto));

        // when
        final Optional<CourseDTO> result = sut.handle(new CourseByUUIDQuery(uuid));

        // then
        assertThat(result).contains(dto);
    }

    @Test
    void handle_courseDoesNotExist_emptyReturned() {
        // given
        final UUID uuid = UUID.randomUUID();
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final Optional<CourseDTO> result = sut.handle(new CourseByUUIDQuery(uuid));

        // then
        assertThat(result).isEmpty();
    }
}
