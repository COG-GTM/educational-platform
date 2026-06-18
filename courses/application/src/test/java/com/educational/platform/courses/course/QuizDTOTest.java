package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class QuizDTOTest {

    @Test
    void tupleConstructor_mapsBaseFields() {
        // given - a quiz row is mapped from the joined tuple via the alias -> index map (aliases out of
        // order to prove index-driven lookup); the course uuid is supplied separately by the transformer
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuple = {
                "Quiz Description",
                7,
                "Quiz Title"
        };
        final Map<String, Integer> aliasToIndexMap = Map.of(
                CurriculumItemDTO.DESCRIPTION, 0,
                CurriculumItemDTO.SERIAL_NUMBER, 1,
                CurriculumItemDTO.TITLE, 2);

        // when
        final QuizDTO dto = new QuizDTO(courseUuid, tuple, aliasToIndexMap);

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("uuid", courseUuid)
                .hasFieldOrPropertyWithValue("title", "Quiz Title")
                .hasFieldOrPropertyWithValue("description", "Quiz Description")
                .hasFieldOrPropertyWithValue("serialNumber", 7);
    }

    @Test
    void tupleConstructor_questionsInitialisedEmpty() {
        // given - questions are not read from the row tuple; the constructor must seed a non-null, empty
        // list so the transformer can attach question DTOs as it walks subsequent joined rows
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final Object[] tuple = {
                "Quiz Title",
                "Quiz Description",
                1
        };
        final Map<String, Integer> aliasToIndexMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2);

        // when
        final QuizDTO dto = new QuizDTO(courseUuid, tuple, aliasToIndexMap);

        // then
        assertThat(dto.questions).isNotNull().isEmpty();
    }
}
