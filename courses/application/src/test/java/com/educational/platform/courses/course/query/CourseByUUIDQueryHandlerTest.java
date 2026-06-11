package com.educational.platform.courses.course.query;

import com.educational.platform.courses.course.CourseDTO;
import com.educational.platform.courses.course.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseByUUIDQueryHandlerTest {

    @Mock
    private CourseRepository repository;

    private CourseByUUIDQueryHandler sut;

    @BeforeEach
    void setUp() {
        sut = new CourseByUUIDQueryHandler(repository);
    }

    @Test
    void handle_existingCourse_returnsCourseDTO() {
        // given
        final UUID uuid = UUID.randomUUID();
        final CourseDTO dto = mock(CourseDTO.class);
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(dto));

        // when
        final Optional<CourseDTO> result = sut.handle(new CourseByUUIDQuery(uuid));

        // then
        assertThat(result).isPresent().contains(dto);
    }

    @Test
    void handle_nonExistentCourse_returnsEmptyOptional() {
        // given
        final UUID uuid = UUID.randomUUID();
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final Optional<CourseDTO> result = sut.handle(new CourseByUUIDQuery(uuid));

        // then
        assertThat(result).isEmpty();
    }
}
