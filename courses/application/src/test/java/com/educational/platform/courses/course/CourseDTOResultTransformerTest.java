package com.educational.platform.courses.course;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseDTOResultTransformerTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    private CourseDTOResultTransformer sut;

    @BeforeEach
    void setUp() {
        sut = new CourseDTOResultTransformer();
    }

    @Test
    void aliasToIndexMap_constructsMapFromAliases() {
        // given
        final String[] aliases = {"col_a", "col_b", "col_c"};

        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(aliases);

        // then
        assertThat(result)
                .containsEntry("col_a", 0)
                .containsEntry("col_b", 1)
                .containsEntry("col_c", 2)
                .hasSize(3);
    }

    @Test
    void aliasToIndexMap_emptyAliases_returnsEmptyMap() {
        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(new String[]{});

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void aliasToIndexMap_preservesInsertionOrder() {
        // given
        final String[] aliases = {"z_col", "a_col", "m_col"};

        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(aliases);

        // then — LinkedHashMap preserves insertion order
        assertThat(result.keySet()).containsExactly("z_col", "a_col", "m_col");
    }

    @Test
    void transformTuple_createsCourseDTO() {
        // given
        final String[] aliases = {
                CourseDTO.UUID_COLUMN, CourseDTO.NAME_COLUMN, CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, CurriculumItemDTO.TYPE
        };
        final Object[] tuple = {
                UUID_VALUE, "name", "description", new NumberOfStudents(5), "Lecture"
        };

        // when
        final CourseDTO dto = sut.transformTuple(tuple, aliases);

        // then
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
        assertThat(dto.name()).isEqualTo("name");
        assertThat(dto.description()).isEqualTo("description");
        assertThat(dto.numberOfStudents()).isEqualTo(5);
    }

    @Test
    void transformTuple_sameUuidCalledTwice_returnsSameDTOInstance() {
        // given
        final String[] aliases = {
                CourseDTO.UUID_COLUMN, CourseDTO.NAME_COLUMN, CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, CurriculumItemDTO.TYPE
        };
        final Object[] tuple1 = {
                UUID_VALUE, "name", "description", new NumberOfStudents(5), "Unknown"
        };
        final Object[] tuple2 = {
                UUID_VALUE, "name", "description", new NumberOfStudents(5), "Unknown"
        };

        // when
        final CourseDTO first = sut.transformTuple(tuple1, aliases);
        final CourseDTO second = sut.transformTuple(tuple2, aliases);

        // then — same object due to computeIfAbsent deduplication
        assertThat(first).isSameAs(second);
    }

    @Test
    void transformTuple_differentUuids_returnsDifferentDTOs() {
        // given
        final UUID anotherUuid = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN, CourseDTO.NAME_COLUMN, CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, CurriculumItemDTO.TYPE
        };
        final Object[] tuple1 = {
                UUID_VALUE, "name1", "desc1", new NumberOfStudents(1), "Unknown"
        };
        final Object[] tuple2 = {
                anotherUuid, "name2", "desc2", new NumberOfStudents(2), "Unknown"
        };

        // when
        final CourseDTO first = sut.transformTuple(tuple1, aliases);
        final CourseDTO second = sut.transformTuple(tuple2, aliases);

        // then
        assertThat(first).isNotSameAs(second);
        assertThat(first.uuid()).isEqualTo(UUID_VALUE);
        assertThat(second.uuid()).isEqualTo(anotherUuid);
    }
}
