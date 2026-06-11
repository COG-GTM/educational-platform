package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LectureDTOTest {

    // --- constructor field mapping tests ---

    @Test
    void constructor_allFieldsMapped() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {"Lecture Title", "Lecture Description", 1, "Lecture body text"};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2,
                LectureDTO.TEXT, 3
        );

        // when
        final LectureDTO sut = new LectureDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("uuid", uuid);
        assertThat(sut).hasFieldOrPropertyWithValue("title", "Lecture Title");
        assertThat(sut).hasFieldOrPropertyWithValue("description", "Lecture Description");
        assertThat(sut).hasFieldOrPropertyWithValue("serialNumber", 1);
        assertThat(sut).hasFieldOrPropertyWithValue("text", "Lecture body text");
    }

    @Test
    void constructor_nullTextValue_storedAsNull() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {"Title", "Desc", 1, null};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2,
                LectureDTO.TEXT, 3
        );

        // when
        final LectureDTO sut = new LectureDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("text", null);
    }

    @Test
    void constructor_nullTitleAndDescription_storedAsNull() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {null, null, 1, "text"};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2,
                LectureDTO.TEXT, 3
        );

        // when
        final LectureDTO sut = new LectureDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("title", null);
        assertThat(sut).hasFieldOrPropertyWithValue("description", null);
    }

    @Test
    void constructor_uuidPassedDirectly_notExtractedFromTuple() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {"Title", "Desc", 1, "text"};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2,
                LectureDTO.TEXT, 3
        );

        // when
        final LectureDTO sut = new LectureDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void constructor_aliasesInDifferentOrder_stillMapsCorrectly() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {"text body", 5, "Title", "Desc"};
        final Map<String, Integer> aliasMap = Map.of(
                LectureDTO.TEXT, 0,
                CurriculumItemDTO.SERIAL_NUMBER, 1,
                CurriculumItemDTO.TITLE, 2,
                CurriculumItemDTO.DESCRIPTION, 3
        );

        // when
        final LectureDTO sut = new LectureDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("title", "Title");
        assertThat(sut).hasFieldOrPropertyWithValue("description", "Desc");
        assertThat(sut).hasFieldOrPropertyWithValue("serialNumber", 5);
        assertThat(sut).hasFieldOrPropertyWithValue("text", "text body");
    }

    @Test
    void constructor_zeroSerialNumber_storedCorrectly() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {"Title", "Desc", 0, "text"};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2,
                LectureDTO.TEXT, 3
        );

        // when
        final LectureDTO sut = new LectureDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("serialNumber", 0);
    }

    // --- structural tests ---

    @Test
    void extendsCurriculumItemDTO() {
        // then
        assertThat(CurriculumItemDTO.class).isAssignableFrom(LectureDTO.class);
    }

    @Test
    void textConstant_hasExpectedValue() {
        // then
        assertThat(LectureDTO.TEXT).isEqualTo("text");
    }

    @Test
    void constructor_emptyStringFields_storedCorrectly() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {"", "", 1, ""};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2,
                LectureDTO.TEXT, 3
        );

        // when
        final LectureDTO sut = new LectureDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("title", "");
        assertThat(sut).hasFieldOrPropertyWithValue("description", "");
        assertThat(sut).hasFieldOrPropertyWithValue("text", "");
    }

    @Test
    void constructor_unicodeContent_storedCorrectly() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {"講義タイトル", "説明", 1, "コンテンツ本文"};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2,
                LectureDTO.TEXT, 3
        );

        // when
        final LectureDTO sut = new LectureDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("title", "講義タイトル");
        assertThat(sut).hasFieldOrPropertyWithValue("text", "コンテンツ本文");
    }
}
