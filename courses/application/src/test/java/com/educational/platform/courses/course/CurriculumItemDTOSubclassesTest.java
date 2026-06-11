package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CurriculumItemDTOSubclassesTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void lectureDTO_constructsFromTuplesAndAlias() {
        // given
        final Object[] tuples = {"Title", "Description", 1, "Lecture text content"};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2,
                LectureDTO.TEXT, 3
        );

        // when
        final LectureDTO dto = new LectureDTO(UUID_VALUE, tuples, aliasMap);

        // then
        assertThat(dto.uuid).isEqualTo(UUID_VALUE);
        assertThat(dto.title).isEqualTo("Title");
        assertThat(dto.description).isEqualTo("Description");
        assertThat(dto.serialNumber).isEqualTo(1);
        assertThat(dto.text).isEqualTo("Lecture text content");
    }

    @Test
    void quizDTO_constructsFromTuplesAndAlias() {
        // given
        final Object[] tuples = {"Quiz Title", "Quiz Description", 2};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2
        );

        // when
        final QuizDTO dto = new QuizDTO(UUID_VALUE, tuples, aliasMap);

        // then
        assertThat(dto.uuid).isEqualTo(UUID_VALUE);
        assertThat(dto.title).isEqualTo("Quiz Title");
        assertThat(dto.description).isEqualTo("Quiz Description");
        assertThat(dto.serialNumber).isEqualTo(2);
        assertThat(dto.questions).isEmpty();
    }

    @Test
    void curriculumItemDTO_constants_haveExpectedValues() {
        assertThat(CurriculumItemDTO.TYPE).isEqualTo("curriculumItems_type");
        assertThat(CurriculumItemDTO.TITLE).isEqualTo("curriculumItems_title");
        assertThat(CurriculumItemDTO.DESCRIPTION).isEqualTo("curriculumItems_description");
        assertThat(CurriculumItemDTO.SERIAL_NUMBER).isEqualTo("curriculumItems_serialNumber");
    }

    @Test
    void lectureDTO_extendsAbstractCurriculumItemDTO() {
        final Object[] tuples = {"T", "D", 0, "text"};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2,
                LectureDTO.TEXT, 3
        );

        final LectureDTO dto = new LectureDTO(UUID_VALUE, tuples, aliasMap);
        assertThat(dto).isInstanceOf(CurriculumItemDTO.class);
    }

    @Test
    void quizDTO_extendsAbstractCurriculumItemDTO() {
        final Object[] tuples = {"T", "D", 0};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2
        );

        final QuizDTO dto = new QuizDTO(UUID_VALUE, tuples, aliasMap);
        assertThat(dto).isInstanceOf(CurriculumItemDTO.class);
    }
}
