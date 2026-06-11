package com.educational.platform.courses.course.query;

import com.educational.platform.courses.course.CourseDTO;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.CurriculumItemDTO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
        final CourseDTO courseDTO = new CourseDTO(uuid, "name", "description", 5, List.of());
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
    void handle_queryDelegatesToRepositoryWithCorrectUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.empty());

        // when
        sut.handle(query);

        // then
        verify(repository).findDTOByUuid(uuid);
    }

    @Test
    void handle_nonExistingCourse_emptyReturned() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void handle_existingCourse_returnedDtoIsSameInstanceFromRepository() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, "course", "desc", 0, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(courseDTO);
    }

    @Test
    void handle_existingCourseWithCurriculumItems_curriculumItemsPreserved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440004");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CurriculumItemDTO item = mock(CurriculumItemDTO.class);
        final CourseDTO courseDTO = new CourseDTO(uuid, "name", "description", 3, List.of(item));
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().curriculumItems()).hasSize(1);
    }

    @Test
    void handle_repositoryCalledExactlyOnce_noExtraInteractions() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440005");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.empty());

        // when
        sut.handle(query);

        // then
        verify(repository).findDTOByUuid(uuid);
        verifyNoMoreInteractions(repository);
    }

}
