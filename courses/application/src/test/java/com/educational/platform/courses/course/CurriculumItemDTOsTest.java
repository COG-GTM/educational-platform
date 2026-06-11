package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests constructors of {@link LectureDTO}, {@link QuizDTO}, and {@link QuestionDTO}.
 */
public class CurriculumItemDTOsTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void lectureDTO_constructedFromTuplesAndAliases() {
        // given
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2,
                LectureDTO.TEXT, 3
        );
        final Object[] tuples = {"Lecture Title", "Lecture Desc", 1, "Lecture Content"};

        // when
        final LectureDTO dto = new LectureDTO(UUID_VALUE, tuples, aliasMap);

        // then
        assertThat(dto.uuid).isEqualTo(UUID_VALUE);
        assertThat(dto.title).isEqualTo("Lecture Title");
        assertThat(dto.description).isEqualTo("Lecture Desc");
        assertThat(dto.serialNumber).isEqualTo(1);
        assertThat(dto.text).isEqualTo("Lecture Content");
    }

    @Test
    void quizDTO_constructedFromTuplesAndAliases() {
        // given
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2
        );
        final Object[] tuples = {"Quiz Title", "Quiz Desc", 2};

        // when
        final QuizDTO dto = new QuizDTO(UUID_VALUE, tuples, aliasMap);

        // then
        assertThat(dto.uuid).isEqualTo(UUID_VALUE);
        assertThat(dto.title).isEqualTo("Quiz Title");
        assertThat(dto.description).isEqualTo("Quiz Desc");
        assertThat(dto.serialNumber).isEqualTo(2);
        assertThat(dto.questions).isEmpty();
    }

    @Test
    void questionDTO_constructedFromContent() {
        // when
        final QuestionDTO dto = new QuestionDTO("What is Java?");

        // then
        assertThat(dto.content).isEqualTo("What is Java?");
    }

    @Test
    void lectureDTO_nullText_handledGracefully() {
        // given
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2,
                LectureDTO.TEXT, 3
        );
        final Object[] tuples = {"Title", "Desc", 1, null};

        // when
        final LectureDTO dto = new LectureDTO(UUID_VALUE, tuples, aliasMap);

        // then
        assertThat(dto.text).isNull();
    }

    @Test
    void curriculumItemDTO_constants_matchExpectedNames() {
        assertThat(CurriculumItemDTO.TYPE).isEqualTo("curriculumItems_type");
        assertThat(CurriculumItemDTO.TITLE).isEqualTo("curriculumItems_title");
        assertThat(CurriculumItemDTO.DESCRIPTION).isEqualTo("curriculumItems_description");
        assertThat(CurriculumItemDTO.SERIAL_NUMBER).isEqualTo("curriculumItems_serialNumber");
    }
}
