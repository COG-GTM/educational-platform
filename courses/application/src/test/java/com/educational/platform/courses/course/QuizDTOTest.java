package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link QuizDTO} tuple-based constructor.
 */
public class QuizDTOTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void constructor_extractsFieldsFromTupleAndAliasMap() {
        // given
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2
        );
        final Object[] tuples = {"Quiz Title", "Quiz Desc", 3};

        // when
        final QuizDTO dto = new QuizDTO(UUID_VALUE, tuples, aliasMap);

        // then
        assertThat(dto.uuid).isEqualTo(UUID_VALUE);
        assertThat(dto.title).isEqualTo("Quiz Title");
        assertThat(dto.description).isEqualTo("Quiz Desc");
        assertThat(dto.serialNumber).isEqualTo(3);
    }

    @Test
    void constructor_initializesEmptyQuestionsList() {
        // given
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2
        );
        final Object[] tuples = {"Title", "Desc", 1};

        // when
        final QuizDTO dto = new QuizDTO(UUID_VALUE, tuples, aliasMap);

        // then
        assertThat(dto.questions).isNotNull().isEmpty();
    }

    @Test
    void isSubclassOfCurriculumItemDTO() {
        // given
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2
        );
        final Object[] tuples = {"T", "D", 1};

        // when
        final QuizDTO dto = new QuizDTO(UUID_VALUE, tuples, aliasMap);

        // then
        assertThat(dto).isInstanceOf(CurriculumItemDTO.class);
    }
}
