package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseDTOTest {

    @Test
    void tupleConstructor_mapsColumnsByAliasIndex() {
        // given - the list-courses read path builds a CourseDTO from a Hibernate tuple keyed by an
        // alias -> column-index map; the aliases are deliberately out of order to prove the mapping is
        // index-driven (via the map) and not positional
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuple = {
                "Course Description",
                new NumberOfStudents(42),
                uuid,
                "Course Name"
        };
        final Map<String, Integer> aliasToIndexMap = Map.of(
                CourseDTO.DESCRIPTION_COLUMN, 0,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 1,
                CourseDTO.UUID_COLUMN, 2,
                CourseDTO.NAME_COLUMN, 3);

        // when
        final CourseDTO dto = new CourseDTO(tuple, aliasToIndexMap);

        // then
        assertThat(dto.uuid()).isEqualTo(uuid);
        assertThat(dto.name()).isEqualTo("Course Name");
        assertThat(dto.description()).isEqualTo("Course Description");
        assertThat(dto.numberOfStudents()).isEqualTo(42);
    }

    @Test
    void tupleConstructor_curriculumItemsInitialisedEmpty() {
        // given - the result transformer appends curriculum-item DTOs to this list as it walks the
        // joined rows, so the constructor must seed a non-null, empty list rather than leaving it null
        final Object[] tuple = {
                UUID.fromString("123e4567-e89b-12d3-a456-426655440002"),
                "Course Name",
                "Course Description",
                new NumberOfStudents(0)
        };
        final Map<String, Integer> aliasToIndexMap = Map.of(
                CourseDTO.UUID_COLUMN, 0,
                CourseDTO.NAME_COLUMN, 1,
                CourseDTO.DESCRIPTION_COLUMN, 2,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 3);

        // when
        final CourseDTO dto = new CourseDTO(tuple, aliasToIndexMap);

        // then
        assertThat(dto.curriculumItems()).isNotNull().isEmpty();
    }
}
