package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CourseDTOTest {

    @Test
    void canonicalConstructor_allFieldsPopulated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final List<CurriculumItemDTO> items = new ArrayList<>();

        // when
        final CourseDTO sut = new CourseDTO(uuid, "Name", "Desc", 10, items);

        // then
        assertThat(sut.uuid()).isEqualTo(uuid);
        assertThat(sut.name()).isEqualTo("Name");
        assertThat(sut.description()).isEqualTo("Desc");
        assertThat(sut.numberOfStudents()).isEqualTo(10);
        assertThat(sut.curriculumItems()).isSameAs(items);
    }

    @Test
    void tupleConstructor_extractsFieldsFromTupleArray() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {uuid, "Course Name", "Course Desc", new NumberOfStudents(15)};
        final Map<String, Integer> aliasMap = Map.of(
                CourseDTO.UUID_COLUMN, 0,
                CourseDTO.NAME_COLUMN, 1,
                CourseDTO.DESCRIPTION_COLUMN, 2,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 3
        );

        // when
        final CourseDTO sut = new CourseDTO(tuples, aliasMap);

        // then
        assertThat(sut.uuid()).isEqualTo(uuid);
        assertThat(sut.name()).isEqualTo("Course Name");
        assertThat(sut.description()).isEqualTo("Course Desc");
        assertThat(sut.numberOfStudents()).isEqualTo(15);
    }

    @Test
    void tupleConstructor_curriculumItemsInitializedAsEmptyList() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {uuid, "Name", "Desc", new NumberOfStudents(0)};
        final Map<String, Integer> aliasMap = Map.of(
                CourseDTO.UUID_COLUMN, 0,
                CourseDTO.NAME_COLUMN, 1,
                CourseDTO.DESCRIPTION_COLUMN, 2,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 3
        );

        // when
        final CourseDTO sut = new CourseDTO(tuples, aliasMap);

        // then
        assertThat(sut.curriculumItems()).isNotNull().isEmpty();
    }

    @Test
    void tupleConstructor_curriculumItemsListIsMutable() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {uuid, "Name", "Desc", new NumberOfStudents(0)};
        final Map<String, Integer> aliasMap = Map.of(
                CourseDTO.UUID_COLUMN, 0,
                CourseDTO.NAME_COLUMN, 1,
                CourseDTO.DESCRIPTION_COLUMN, 2,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 3
        );
        final CourseDTO sut = new CourseDTO(tuples, aliasMap);

        // when — adding an item should not throw
        final QuizDTO quizDTO = new QuizDTO(uuid, new Object[]{"Title", "Desc", 1}, Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2
        ));
        sut.curriculumItems().add(quizDTO);

        // then
        assertThat(sut.curriculumItems()).hasSize(1);
    }

    @Test
    void tupleConstructor_numberOfStudentsExtractedFromValueObject() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {uuid, "Name", "Desc", new NumberOfStudents(999)};
        final Map<String, Integer> aliasMap = Map.of(
                CourseDTO.UUID_COLUMN, 0,
                CourseDTO.NAME_COLUMN, 1,
                CourseDTO.DESCRIPTION_COLUMN, 2,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 3
        );

        // when
        final CourseDTO sut = new CourseDTO(tuples, aliasMap);

        // then
        assertThat(sut.numberOfStudents()).isEqualTo(999);
    }

    @Test
    void tupleConstructor_nullNameAndDescription_storedAsNull() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {uuid, null, null, new NumberOfStudents(0)};
        final Map<String, Integer> aliasMap = Map.of(
                CourseDTO.UUID_COLUMN, 0,
                CourseDTO.NAME_COLUMN, 1,
                CourseDTO.DESCRIPTION_COLUMN, 2,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 3
        );

        // when
        final CourseDTO sut = new CourseDTO(tuples, aliasMap);

        // then
        assertThat(sut.name()).isNull();
        assertThat(sut.description()).isNull();
    }

    @Test
    void tupleConstructor_nullNumberOfStudents_throwsNPE() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {uuid, "Name", "Desc", null};
        final Map<String, Integer> aliasMap = Map.of(
                CourseDTO.UUID_COLUMN, 0,
                CourseDTO.NAME_COLUMN, 1,
                CourseDTO.DESCRIPTION_COLUMN, 2,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 3
        );

        // when / then
        assertThatThrownBy(() -> new CourseDTO(tuples, aliasMap))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void staticConstants_haveExpectedValues() {
        // then
        assertThat(CourseDTO.UUID_COLUMN).isEqualTo("course_uuid");
        assertThat(CourseDTO.NAME_COLUMN).isEqualTo("course_name");
        assertThat(CourseDTO.DESCRIPTION_COLUMN).isEqualTo("course_description");
        assertThat(CourseDTO.NUMBER_OF_STUDENTS_COLUMN).isEqualTo("course_numberOfStudents");
    }

    @Test
    void tupleConstructor_aliasesInDifferentOrder_stillMapsCorrectly() {
        // given — aliases map columns to non-sequential positions
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {new NumberOfStudents(7), "Desc", uuid, "Name"};
        final Map<String, Integer> aliasMap = Map.of(
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 0,
                CourseDTO.DESCRIPTION_COLUMN, 1,
                CourseDTO.UUID_COLUMN, 2,
                CourseDTO.NAME_COLUMN, 3
        );

        // when
        final CourseDTO sut = new CourseDTO(tuples, aliasMap);

        // then
        assertThat(sut.uuid()).isEqualTo(uuid);
        assertThat(sut.name()).isEqualTo("Name");
        assertThat(sut.description()).isEqualTo("Desc");
        assertThat(sut.numberOfStudents()).isEqualTo(7);
    }

    @Test
    void recordEquality_sameFieldValues_areEqual() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final List<CurriculumItemDTO> items = new ArrayList<>();
        final CourseDTO a = new CourseDTO(uuid, "Name", "Desc", 5, items);
        final CourseDTO b = new CourseDTO(uuid, "Name", "Desc", 5, items);

        // then
        assertThat(a).isEqualTo(b);
    }
}
