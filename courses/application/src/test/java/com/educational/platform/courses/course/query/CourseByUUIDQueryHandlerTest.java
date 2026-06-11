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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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

    @Test
    void handle_nullUuidInQuery_delegatesToRepository() {
        // given
        final CourseByUUIDQuery query = new CourseByUUIDQuery(null);
        when(repository.findDTOByUuid(null)).thenReturn(Optional.empty());

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isEmpty();
        verify(repository).findDTOByUuid(null);
    }

    @Test
    void handle_calledTwiceWithDifferentUuids_delegatesBothCalls() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440006");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440007");
        final CourseDTO dto1 = new CourseDTO(uuid1, "course1", "desc1", 1, List.of());
        final CourseDTO dto2 = new CourseDTO(uuid2, "course2", "desc2", 2, List.of());
        when(repository.findDTOByUuid(uuid1)).thenReturn(Optional.of(dto1));
        when(repository.findDTOByUuid(uuid2)).thenReturn(Optional.of(dto2));

        // when
        final Optional<CourseDTO> result1 = sut.handle(new CourseByUUIDQuery(uuid1));
        final Optional<CourseDTO> result2 = sut.handle(new CourseByUUIDQuery(uuid2));

        // then
        assertThat(result1).isPresent();
        assertThat(result1.get().name()).isEqualTo("course1");
        assertThat(result2).isPresent();
        assertThat(result2.get().name()).isEqualTo("course2");
        verify(repository).findDTOByUuid(uuid1);
        verify(repository).findDTOByUuid(uuid2);
    }

    @Test
    void handle_existingCourseWithMultipleCurriculumItems_allItemsPreserved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440008");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CurriculumItemDTO item1 = mock(CurriculumItemDTO.class);
        final CurriculumItemDTO item2 = mock(CurriculumItemDTO.class);
        final CurriculumItemDTO item3 = mock(CurriculumItemDTO.class);
        final CourseDTO courseDTO = new CourseDTO(uuid, "name", "desc", 10, List.of(item1, item2, item3));
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().curriculumItems()).hasSize(3);
        assertThat(result.get().curriculumItems()).containsExactly(item1, item2, item3);
    }

    @Test
    void handle_courseWithZeroStudents_returnsCorrectly() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440009");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, "empty course", "no students", 0, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().numberOfStudents()).isZero();
        assertThat(result.get().name()).isEqualTo("empty course");
    }

    @Test
    void handle_existingCourse_allRecordFieldsAccessible() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440010");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, "Java 101", "Intro to Java", 42, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        final CourseDTO returned = result.get();
        assertThat(returned.uuid()).isEqualTo(uuid);
        assertThat(returned.name()).isEqualTo("Java 101");
        assertThat(returned.description()).isEqualTo("Intro to Java");
        assertThat(returned.numberOfStudents()).isEqualTo(42);
        assertThat(returned.curriculumItems()).isEmpty();
    }

    @Test
    void handle_nullQuery_throwsNullPointerException() {
        // when / then
        assertThatThrownBy(() -> sut.handle(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void handle_resultOptionalIsDirectlyFromRepository_notRewrapped() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440011");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final Optional<CourseDTO> repositoryResult = Optional.of(
                new CourseDTO(uuid, "course", "desc", 1, List.of()));
        when(repository.findDTOByUuid(uuid)).thenReturn(repositoryResult);

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isSameAs(repositoryResult);
    }

    @Test
    void handle_courseWithNullName_returnedAsIs() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440012");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, null, null, 0, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().name()).isNull();
        assertThat(result.get().description()).isNull();
    }

    @Test
    void handle_courseWithMaxIntStudents_returnedAsIs() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440013");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, "popular", "desc", Integer.MAX_VALUE, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().numberOfStudents()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void handle_courseWithNegativeStudents_returnedAsIs() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440014");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, "negative", "desc", -1, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().numberOfStudents()).isEqualTo(-1);
    }

    @Test
    void handle_courseWithEmptyCurriculumItemsList_emptyListPreserved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440015");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, "empty items", "desc", 5, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().curriculumItems()).isEmpty();
    }

    @Test
    void handle_repositoryThrowsException_exceptionPropagated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440016");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        when(repository.findDTOByUuid(uuid)).thenThrow(new RuntimeException("db error"));

        // when / then
        assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db error");
    }

    @Test
    void handle_sameUuidCalledTwice_bothCallsDelegated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440017");
        final CourseDTO dto = new CourseDTO(uuid, "course", "desc", 1, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(dto));

        // when
        final Optional<CourseDTO> result1 = sut.handle(new CourseByUUIDQuery(uuid));
        final Optional<CourseDTO> result2 = sut.handle(new CourseByUUIDQuery(uuid));

        // then
        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        assertThat(result1.get()).isSameAs(result2.get());
        verify(repository, times(2)).findDTOByUuid(uuid);
    }

    @Test
    void handle_firstCallFound_secondCallEmpty_bothDelegated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440018");
        final CourseDTO dto = new CourseDTO(uuid, "course", "desc", 1, List.of());
        when(repository.findDTOByUuid(uuid))
                .thenReturn(Optional.of(dto))
                .thenReturn(Optional.empty());

        // when
        final Optional<CourseDTO> result1 = sut.handle(new CourseByUUIDQuery(uuid));
        final Optional<CourseDTO> result2 = sut.handle(new CourseByUUIDQuery(uuid));

        // then
        assertThat(result1).isPresent();
        assertThat(result2).isEmpty();
        verify(repository, times(2)).findDTOByUuid(uuid);
    }

    @Test
    void handle_courseWithLongNameAndDescription_returnedAsIs() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440019");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final String longName = "A".repeat(1000);
        final String longDesc = "B".repeat(5000);
        final CourseDTO courseDTO = new CourseDTO(uuid, longName, longDesc, 1, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().name()).hasSize(1000);
        assertThat(result.get().description()).hasSize(5000);
    }

    @Test
    void handle_courseWithNullDescription_namePreserved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440020");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, "present-name", null, 10, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("present-name");
        assertThat(result.get().description()).isNull();
    }

    @Test
    void handle_repositoryThrowsIllegalArgumentException_exceptionPropagated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440021");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        when(repository.findDTOByUuid(uuid)).thenThrow(new IllegalArgumentException("invalid uuid"));

        // when / then
        assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid uuid");
    }

    @Test
    void handle_courseWithMinIntStudents_returnedAsIs() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440022");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, "min-students", "desc", Integer.MIN_VALUE, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().numberOfStudents()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void handle_courseWithEmptyStringFields_returnedAsIs() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440023");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, "", "", 0, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().name()).isEmpty();
        assertThat(result.get().description()).isEmpty();
    }

    @Test
    void handle_uuidObjectPassedDirectlyToRepository_sameReference() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440024");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.empty());

        // when
        sut.handle(query);

        // then
        verify(repository).findDTOByUuid(uuid);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void handle_courseWithNullCurriculumItemsList_returnedAsIs() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440025");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, "name", "desc", 5, null);
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().curriculumItems()).isNull();
    }

    @Test
    void handle_courseWithWhitespaceOnlyNameAndDescription_returnedAsIs() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440026");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, "   ", "\t\n", 0, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("   ");
        assertThat(result.get().description()).isEqualTo("\t\n");
    }

    @Test
    void handle_randomUuid_delegatedToRepository() {
        // given
        final UUID uuid = UUID.randomUUID();
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, "course", "desc", 1, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().uuid()).isEqualTo(uuid);
        verify(repository).findDTOByUuid(uuid);
    }

    @Test
    void handle_repositoryThrowsUnsupportedOperationException_exceptionPropagated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440027");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        when(repository.findDTOByUuid(uuid)).thenThrow(new UnsupportedOperationException("not implemented"));

        // when / then
        assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("not implemented");
    }

    @Test
    void handle_consecutiveCallsForDifferentUuids_noCrossContamination() {
        // given
        final UUID uuid1 = UUID.randomUUID();
        final UUID uuid2 = UUID.randomUUID();
        final CourseDTO dto1 = new CourseDTO(uuid1, "first", "desc1", 10, List.of());
        final CourseDTO dto2 = new CourseDTO(uuid2, "second", "desc2", 20, List.of());
        when(repository.findDTOByUuid(uuid1)).thenReturn(Optional.of(dto1));
        when(repository.findDTOByUuid(uuid2)).thenReturn(Optional.of(dto2));

        // when
        final Optional<CourseDTO> result1 = sut.handle(new CourseByUUIDQuery(uuid1));
        final Optional<CourseDTO> result2 = sut.handle(new CourseByUUIDQuery(uuid2));

        // then
        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        assertThat(result1.get().uuid()).isEqualTo(uuid1);
        assertThat(result2.get().uuid()).isEqualTo(uuid2);
        assertThat(result1.get().name()).isEqualTo("first");
        assertThat(result2.get().name()).isEqualTo("second");
    }

    @Test
    void handle_courseWithUnicodeNameAndDescription_returnedAsIs() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440028");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, "日本語コース", "Описание курса αβγ", 7, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("日本語コース");
        assertThat(result.get().description()).isEqualTo("Описание курса αβγ");
    }

    @Test
    void handle_courseWithNullStudents_returnedAsIs() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440029");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, "name", "desc", 0, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().numberOfStudents()).isZero();
        assertThat(result.get()).isSameAs(courseDTO);
    }

    @Test
    void handle_courseWithOneStudent_returnedAsIs() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440030");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CourseDTO courseDTO = new CourseDTO(uuid, "single student course", "desc", 1, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().numberOfStudents()).isEqualTo(1);
    }

    @Test
    void handle_courseWithNullUuidInDTO_returnedAsIs() {
        // given
        final UUID queryUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440031");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(queryUuid);
        final CourseDTO courseDTO = new CourseDTO(null, "name", "desc", 5, List.of());
        when(repository.findDTOByUuid(queryUuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().uuid()).isNull();
        assertThat(result.get().name()).isEqualTo("name");
    }

    @Test
    void handle_repositoryThrowsNullPointerException_exceptionPropagated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440032");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        when(repository.findDTOByUuid(uuid)).thenThrow(new NullPointerException("null repo"));

        // when / then
        assertThatThrownBy(() -> sut.handle(query))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("null repo");
    }

    @Test
    void handle_courseWithMixedNullAndNonNullCurriculumItems_allPreserved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440033");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final CurriculumItemDTO item = mock(CurriculumItemDTO.class);
        final java.util.ArrayList<CurriculumItemDTO> items = new java.util.ArrayList<>();
        items.add(item);
        items.add(null);
        final CourseDTO courseDTO = new CourseDTO(uuid, "name", "desc", 2, items);
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().curriculumItems()).hasSize(2);
        assertThat(result.get().curriculumItems().get(0)).isSameAs(item);
        assertThat(result.get().curriculumItems().get(1)).isNull();
    }

    @Test
    void handle_repositoryReturnsNull_nullPropagated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440034");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        when(repository.findDTOByUuid(uuid)).thenReturn(null);

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isNull();
    }

    @Test
    void handle_equivalentUuidObjects_bothDelegatedCorrectly() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440035");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440035");
        assertThat(uuid1).isEqualTo(uuid2);
        assertThat(uuid1).isNotSameAs(uuid2);

        final CourseDTO courseDTO = new CourseDTO(uuid1, "name", "desc", 1, List.of());
        when(repository.findDTOByUuid(uuid1)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(new CourseByUUIDQuery(uuid2));

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(courseDTO);
        verify(repository).findDTOByUuid(uuid2);
    }

    @Test
    void handle_courseWithSpecialCharsInName_returnedAsIs() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440036");
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);
        final String specialName = "C++ & Java <> \"Rust\" 'Go' \\ /path/to/course";
        final CourseDTO courseDTO = new CourseDTO(uuid, specialName, "desc", 1, List.of());
        when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(courseDTO));

        // when
        final Optional<CourseDTO> result = sut.handle(query);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo(specialName);
    }

    @Test
    void handle_handlerIsStateless_identicalCallsReturnFreshRepositoryResult() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440037");
        final CourseDTO dto1 = new CourseDTO(uuid, "v1", "desc1", 1, List.of());
        final CourseDTO dto2 = new CourseDTO(uuid, "v2", "desc2", 2, List.of());
        when(repository.findDTOByUuid(uuid))
                .thenReturn(Optional.of(dto1))
                .thenReturn(Optional.of(dto2));

        // when
        final Optional<CourseDTO> result1 = sut.handle(new CourseByUUIDQuery(uuid));
        final Optional<CourseDTO> result2 = sut.handle(new CourseByUUIDQuery(uuid));

        // then
        assertThat(result1.get().name()).isEqualTo("v1");
        assertThat(result2.get().name()).isEqualTo("v2");
        verify(repository, times(2)).findDTOByUuid(uuid);
    }

}
