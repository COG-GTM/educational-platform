package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CourseDTOTest {

    @Test
    void canonicalConstructor_validArguments_dtoCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseDTO sut = new CourseDTO(uuid, "course", "desc", 5, new ArrayList<>());

        // then
        assertThat(sut.uuid()).isEqualTo(uuid);
        assertThat(sut.name()).isEqualTo("course");
        assertThat(sut.description()).isEqualTo("desc");
        assertThat(sut.numberOfStudents()).isEqualTo(5);
        assertThat(sut.curriculumItems()).isEmpty();
    }

    @Test
    void tupleConstructor_validTuples_dtoCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Map<String, Integer> aliasToIndexMap = new LinkedHashMap<>();
        aliasToIndexMap.put(CourseDTO.UUID_COLUMN, 0);
        aliasToIndexMap.put(CourseDTO.NAME_COLUMN, 1);
        aliasToIndexMap.put(CourseDTO.DESCRIPTION_COLUMN, 2);
        aliasToIndexMap.put(CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 3);

        final Object[] tuples = new Object[]{uuid, "course name", "description", new NumberOfStudents(10)};

        // when
        final CourseDTO sut = new CourseDTO(tuples, aliasToIndexMap);

        // then
        assertThat(sut.uuid()).isEqualTo(uuid);
        assertThat(sut.name()).isEqualTo("course name");
        assertThat(sut.description()).isEqualTo("description");
        assertThat(sut.numberOfStudents()).isEqualTo(10);
        assertThat(sut.curriculumItems()).isEmpty();
    }

    @Test
    void tupleConstructor_zeroStudents_dtoCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final Map<String, Integer> aliasToIndexMap = new LinkedHashMap<>();
        aliasToIndexMap.put(CourseDTO.UUID_COLUMN, 0);
        aliasToIndexMap.put(CourseDTO.NAME_COLUMN, 1);
        aliasToIndexMap.put(CourseDTO.DESCRIPTION_COLUMN, 2);
        aliasToIndexMap.put(CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 3);

        final Object[] tuples = new Object[]{uuid, "course", "desc", new NumberOfStudents(0)};

        // when
        final CourseDTO sut = new CourseDTO(tuples, aliasToIndexMap);

        // then
        assertThat(sut.numberOfStudents()).isZero();
    }

    @Test
    void curriculumItems_mutableList_itemsCanBeAdded() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Map<String, Integer> courseAliasMap = new LinkedHashMap<>();
        courseAliasMap.put(CourseDTO.UUID_COLUMN, 0);
        courseAliasMap.put(CourseDTO.NAME_COLUMN, 1);
        courseAliasMap.put(CourseDTO.DESCRIPTION_COLUMN, 2);
        courseAliasMap.put(CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 3);

        final Object[] tuples = new Object[]{uuid, "name", "desc", new NumberOfStudents(0)};
        final CourseDTO sut = new CourseDTO(tuples, courseAliasMap);

        final Map<String, Integer> itemAliasMap = new LinkedHashMap<>();
        itemAliasMap.put(CurriculumItemDTO.TITLE, 0);
        itemAliasMap.put(CurriculumItemDTO.DESCRIPTION, 1);
        itemAliasMap.put(CurriculumItemDTO.SERIAL_NUMBER, 2);
        final Object[] itemTuples = new Object[]{"Quiz", "desc", 1};

        // when
        sut.curriculumItems().add(new QuizDTO(uuid, itemTuples, itemAliasMap));

        // then
        assertThat(sut.curriculumItems()).hasSize(1);
    }

    @Test
    void constants_correctValues() {
        // when / then
        assertThat(CourseDTO.UUID_COLUMN).isEqualTo("course_uuid");
        assertThat(CourseDTO.NAME_COLUMN).isEqualTo("course_name");
        assertThat(CourseDTO.DESCRIPTION_COLUMN).isEqualTo("course_description");
        assertThat(CourseDTO.NUMBER_OF_STUDENTS_COLUMN).isEqualTo("course_numberOfStudents");
    }
}
