package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class QuizDTOTest {

    @Test
    void constructor_validTuples_quizDtoCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Map<String, Integer> aliasToIndexMap = new LinkedHashMap<>();
        aliasToIndexMap.put(CurriculumItemDTO.TITLE, 0);
        aliasToIndexMap.put(CurriculumItemDTO.DESCRIPTION, 1);
        aliasToIndexMap.put(CurriculumItemDTO.SERIAL_NUMBER, 2);

        final Object[] tuples = new Object[]{"Quiz Title", "Quiz Desc", 1};

        // when
        final QuizDTO sut = new QuizDTO(uuid, tuples, aliasToIndexMap);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("title", "Quiz Title")
                .hasFieldOrPropertyWithValue("description", "Quiz Desc")
                .hasFieldOrPropertyWithValue("serialNumber", 1);
        assertThat(sut.questions).isEmpty();
    }

    @Test
    void constructor_questionsListMutable_canAddQuestions() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Map<String, Integer> aliasToIndexMap = new LinkedHashMap<>();
        aliasToIndexMap.put(CurriculumItemDTO.TITLE, 0);
        aliasToIndexMap.put(CurriculumItemDTO.DESCRIPTION, 1);
        aliasToIndexMap.put(CurriculumItemDTO.SERIAL_NUMBER, 2);

        final Object[] tuples = new Object[]{"Quiz Title", "Quiz Desc", 1};
        final QuizDTO sut = new QuizDTO(uuid, tuples, aliasToIndexMap);

        // when
        sut.questions.add(new QuestionDTO("What is DDD?"));

        // then
        assertThat(sut.questions).hasSize(1);
    }
}
