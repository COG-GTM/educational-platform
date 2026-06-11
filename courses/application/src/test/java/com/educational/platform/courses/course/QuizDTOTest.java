package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class QuizDTOTest {

    // --- constructor field mapping tests ---

    @Test
    void constructor_allFieldsMapped() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {"Quiz Title", "Quiz Description", 1};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2
        );

        // when
        final QuizDTO sut = new QuizDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("uuid", uuid);
        assertThat(sut).hasFieldOrPropertyWithValue("title", "Quiz Title");
        assertThat(sut).hasFieldOrPropertyWithValue("description", "Quiz Description");
        assertThat(sut).hasFieldOrPropertyWithValue("serialNumber", 1);
    }

    @Test
    void constructor_questionsListInitializedAsEmpty() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {"Title", "Desc", 1};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2
        );

        // when
        final QuizDTO sut = new QuizDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("questions", new java.util.ArrayList<>());
    }

    @Test
    void constructor_questionsListIsMutable() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {"Title", "Desc", 1};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2
        );
        final QuizDTO sut = new QuizDTO(uuid, tuples, aliasMap);

        // when
        sut.questions.add(new QuestionDTO("Q1"));

        // then
        assertThat(sut.questions).hasSize(1);
    }

    @Test
    void constructor_nullTitleAndDescription_storedAsNull() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {null, null, 1};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2
        );

        // when
        final QuizDTO sut = new QuizDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("title", null);
        assertThat(sut).hasFieldOrPropertyWithValue("description", null);
    }

    @Test
    void constructor_uuidPassedDirectly_notExtractedFromTuple() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {"Title", "Desc", 1};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2
        );

        // when
        final QuizDTO sut = new QuizDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void constructor_aliasesInDifferentOrder_stillMapsCorrectly() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {5, "Title", "Description"};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.SERIAL_NUMBER, 0,
                CurriculumItemDTO.TITLE, 1,
                CurriculumItemDTO.DESCRIPTION, 2
        );

        // when
        final QuizDTO sut = new QuizDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("title", "Title");
        assertThat(sut).hasFieldOrPropertyWithValue("description", "Description");
        assertThat(sut).hasFieldOrPropertyWithValue("serialNumber", 5);
    }

    @Test
    void constructor_zeroSerialNumber_storedCorrectly() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {"Title", "Desc", 0};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2
        );

        // when
        final QuizDTO sut = new QuizDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("serialNumber", 0);
    }

    // --- structural tests ---

    @Test
    void extendsCurriculumItemDTO() {
        // then
        assertThat(CurriculumItemDTO.class).isAssignableFrom(QuizDTO.class);
    }

    @Test
    void constructor_emptyStringFields_storedCorrectly() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {"", "", 1};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2
        );

        // when
        final QuizDTO sut = new QuizDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("title", "");
        assertThat(sut).hasFieldOrPropertyWithValue("description", "");
    }
}
