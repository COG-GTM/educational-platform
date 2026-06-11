package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the tuple-based constructor of {@link CourseDTO} used by the Hibernate result transformer.
 */
public class CourseDTOTupleConstructorTest {

    @Test
    void tupleConstructor_mapsFieldsCorrectly() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Map<String, Integer> aliasToIndexMap = new LinkedHashMap<>();
        aliasToIndexMap.put(CourseDTO.UUID_COLUMN, 0);
        aliasToIndexMap.put(CourseDTO.NAME_COLUMN, 1);
        aliasToIndexMap.put(CourseDTO.DESCRIPTION_COLUMN, 2);
        aliasToIndexMap.put(CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 3);

        final Object[] tuples = {uuid, "Mathematics", "Advanced course", new NumberOfStudents(42)};

        // when
        final CourseDTO dto = new CourseDTO(tuples, aliasToIndexMap);

        // then
        assertThat(dto.uuid()).isEqualTo(uuid);
        assertThat(dto.name()).isEqualTo("Mathematics");
        assertThat(dto.description()).isEqualTo("Advanced course");
        assertThat(dto.numberOfStudents()).isEqualTo(42);
        assertThat(dto.curriculumItems()).isEmpty();
    }

    @Test
    void tupleConstructor_zeroStudents() {
        // given
        final UUID uuid = UUID.randomUUID();
        final Map<String, Integer> aliasToIndexMap = new LinkedHashMap<>();
        aliasToIndexMap.put(CourseDTO.UUID_COLUMN, 0);
        aliasToIndexMap.put(CourseDTO.NAME_COLUMN, 1);
        aliasToIndexMap.put(CourseDTO.DESCRIPTION_COLUMN, 2);
        aliasToIndexMap.put(CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 3);

        final Object[] tuples = {uuid, "Empty Course", "No students", new NumberOfStudents(0)};

        // when
        final CourseDTO dto = new CourseDTO(tuples, aliasToIndexMap);

        // then
        assertThat(dto.numberOfStudents()).isZero();
        assertThat(dto.curriculumItems()).isNotNull().isEmpty();
    }

    @Test
    void columnConstants_haveExpectedValues() {
        assertThat(CourseDTO.UUID_COLUMN).isEqualTo("course_uuid");
        assertThat(CourseDTO.NAME_COLUMN).isEqualTo("course_name");
        assertThat(CourseDTO.DESCRIPTION_COLUMN).isEqualTo("course_description");
        assertThat(CourseDTO.NUMBER_OF_STUDENTS_COLUMN).isEqualTo("course_numberOfStudents");
    }
}
