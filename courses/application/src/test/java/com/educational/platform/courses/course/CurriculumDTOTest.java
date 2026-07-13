package com.educational.platform.courses.course;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CurriculumDTOTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void courseDTO_fromTuple_fieldsMapped() {
        // given
        final Object[] tuple = {UUID_VALUE, "name", "description", new NumberOfStudents(7)};
        final Map<String, Integer> aliasToIndex = Map.of(
                CourseDTO.UUID_COLUMN, 0,
                CourseDTO.NAME_COLUMN, 1,
                CourseDTO.DESCRIPTION_COLUMN, 2,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 3);

        // when
        final CourseDTO dto = new CourseDTO(tuple, aliasToIndex);

        // then
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
        assertThat(dto.name()).isEqualTo("name");
        assertThat(dto.description()).isEqualTo("description");
        assertThat(dto.numberOfStudents()).isEqualTo(7);
        assertThat(dto.curriculumItems()).isEmpty();
    }

    @Test
    void courseLightDTO_fromNumberOfStudents_fieldsMapped() {
        // when
        final CourseLightDTO dto = new CourseLightDTO(UUID_VALUE, "name", "description", new NumberOfStudents(3));

        // then
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
        assertThat(dto.name()).isEqualTo("name");
        assertThat(dto.description()).isEqualTo("description");
        assertThat(dto.numberOfStudents()).isEqualTo(3);
    }

    @Test
    void lectureDTO_fromTuple_fieldsMapped() {
        // given
        final Object[] tuple = {"lecture title", "lecture description", 1, "lecture text"};
        final Map<String, Integer> aliasToIndex = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2,
                LectureDTO.TEXT, 3);

        // when
        final LectureDTO dto = new LectureDTO(UUID_VALUE, tuple, aliasToIndex);

        // then
        assertThat(dto.uuid).isEqualTo(UUID_VALUE);
        assertThat(dto.title).isEqualTo("lecture title");
        assertThat(dto.description).isEqualTo("lecture description");
        assertThat(dto.serialNumber).isEqualTo(1);
        assertThat(dto.text).isEqualTo("lecture text");
    }

    @Test
    void quizDTO_fromTuple_fieldsMapped() {
        // given
        final Object[] tuple = {"quiz title", "quiz description", 2};
        final Map<String, Integer> aliasToIndex = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2);

        // when
        final QuizDTO dto = new QuizDTO(UUID_VALUE, tuple, aliasToIndex);

        // then
        assertThat(dto.uuid).isEqualTo(UUID_VALUE);
        assertThat(dto.title).isEqualTo("quiz title");
        assertThat(dto.description).isEqualTo("quiz description");
        assertThat(dto.serialNumber).isEqualTo(2);
        assertThat(dto.questions).isEmpty();
    }

    @Test
    void questionDTO_created_contentStored() {
        // when
        final QuestionDTO dto = new QuestionDTO("what is java?");

        // then
        assertThat(dto.content).isEqualTo("what is java?");
    }

    @Test
    void lectureType_hasTextValue() {
        // then
        assertThat(LectureType.valueOf("TEXT")).isEqualTo(LectureType.TEXT);
        assertThat(LectureType.values()).containsExactly(LectureType.TEXT);
    }
}
