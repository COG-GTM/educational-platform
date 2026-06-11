package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseDTOTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void primaryConstructor_storesAllFields() {
        // when
        final CourseDTO dto = new CourseDTO(UUID_VALUE, "Math", "Intro", 10, List.of());

        // then
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
        assertThat(dto.name()).isEqualTo("Math");
        assertThat(dto.description()).isEqualTo("Intro");
        assertThat(dto.numberOfStudents()).isEqualTo(10);
        assertThat(dto.curriculumItems()).isEmpty();
    }

    @Test
    void tuplesConstructor_extractsFieldsFromAliasMap() {
        // given
        final Object[] tuples = {UUID_VALUE, "Science", "Desc", new NumberOfStudents(5)};
        final Map<String, Integer> aliasMap = Map.of(
                CourseDTO.UUID_COLUMN, 0,
                CourseDTO.NAME_COLUMN, 1,
                CourseDTO.DESCRIPTION_COLUMN, 2,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 3
        );

        // when
        final CourseDTO dto = new CourseDTO(tuples, aliasMap);

        // then
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
        assertThat(dto.name()).isEqualTo("Science");
        assertThat(dto.description()).isEqualTo("Desc");
        assertThat(dto.numberOfStudents()).isEqualTo(5);
        assertThat(dto.curriculumItems()).isInstanceOf(ArrayList.class).isEmpty();
    }

    @Test
    void constants_haveExpectedValues() {
        assertThat(CourseDTO.UUID_COLUMN).isEqualTo("course_uuid");
        assertThat(CourseDTO.NAME_COLUMN).isEqualTo("course_name");
        assertThat(CourseDTO.DESCRIPTION_COLUMN).isEqualTo("course_description");
        assertThat(CourseDTO.NUMBER_OF_STUDENTS_COLUMN).isEqualTo("course_numberOfStudents");
    }

    @Test
    void equalityAndHashCode_sameValues_equal() {
        final List<CurriculumItemDTO> items = List.of();
        final CourseDTO first = new CourseDTO(UUID_VALUE, "Course", "Desc", 3, items);
        final CourseDTO second = new CourseDTO(UUID_VALUE, "Course", "Desc", 3, items);
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }
}
