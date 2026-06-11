package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CoursesDtoTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void courseLightDTO_convenienceConstructor_unwrapsNumberOfStudents() {
        // when
        final CourseLightDTO dto = new CourseLightDTO(UUID_VALUE, "name", "description", new NumberOfStudents(9));

        // then
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
        assertThat(dto.name()).isEqualTo("name");
        assertThat(dto.description()).isEqualTo("description");
        assertThat(dto.numberOfStudents()).isEqualTo(9);
    }

    @Test
    void courseDTO_tupleConstructor_mapsColumns() {
        // given
        final Map<String, Integer> aliasToIndex = new HashMap<>();
        aliasToIndex.put(CourseDTO.UUID_COLUMN, 0);
        aliasToIndex.put(CourseDTO.NAME_COLUMN, 1);
        aliasToIndex.put(CourseDTO.DESCRIPTION_COLUMN, 2);
        aliasToIndex.put(CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 3);
        final Object[] tuples = {UUID_VALUE, "name", "description", new NumberOfStudents(12)};

        // when
        final CourseDTO dto = new CourseDTO(tuples, aliasToIndex);

        // then
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
        assertThat(dto.name()).isEqualTo("name");
        assertThat(dto.description()).isEqualTo("description");
        assertThat(dto.numberOfStudents()).isEqualTo(12);
        assertThat(dto.curriculumItems()).isEmpty();
    }

    @Test
    void questionDTO_storesContent() {
        // when
        final QuestionDTO dto = new QuestionDTO("content");

        // then
        assertThat(dto).hasFieldOrPropertyWithValue("content", "content");
    }

    @Test
    void lectureDTO_tupleConstructor_mapsFields() {
        // given
        final Map<String, Integer> aliasToIndex = new HashMap<>();
        aliasToIndex.put(CurriculumItemDTO.TITLE, 0);
        aliasToIndex.put(CurriculumItemDTO.DESCRIPTION, 1);
        aliasToIndex.put(CurriculumItemDTO.SERIAL_NUMBER, 2);
        aliasToIndex.put(LectureDTO.TEXT, 3);
        final Object[] tuples = {"title", "description", 1, "text"};

        // when
        final LectureDTO dto = new LectureDTO(UUID_VALUE, tuples, aliasToIndex);

        // then
        assertThat(dto).hasFieldOrPropertyWithValue("title", "title")
                .hasFieldOrPropertyWithValue("description", "description")
                .hasFieldOrPropertyWithValue("serialNumber", 1)
                .hasFieldOrPropertyWithValue("text", "text");
    }

    @Test
    void quizDTO_tupleConstructor_mapsFieldsAndInitializesQuestions() {
        // given
        final Map<String, Integer> aliasToIndex = new HashMap<>();
        aliasToIndex.put(CurriculumItemDTO.TITLE, 0);
        aliasToIndex.put(CurriculumItemDTO.DESCRIPTION, 1);
        aliasToIndex.put(CurriculumItemDTO.SERIAL_NUMBER, 2);
        final Object[] tuples = {"title", "description", 2};

        // when
        final QuizDTO dto = new QuizDTO(UUID_VALUE, tuples, aliasToIndex);

        // then
        assertThat(dto).hasFieldOrPropertyWithValue("title", "title")
                .hasFieldOrPropertyWithValue("serialNumber", 2);
        assertThat(dto).hasFieldOrPropertyWithValue("questions", new java.util.ArrayList<>());
    }
}
