package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link LectureDTO} tuple-based constructor.
 */
public class LectureDTOTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void constructor_extractsFieldsFromTupleAndAliasMap() {
        // given
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2,
                LectureDTO.TEXT, 3
        );
        final Object[] tuples = {"Lecture Title", "Lecture Desc", 1, "Content body"};

        // when
        final LectureDTO dto = new LectureDTO(UUID_VALUE, tuples, aliasMap);

        // then
        assertThat(dto.uuid).isEqualTo(UUID_VALUE);
        assertThat(dto.title).isEqualTo("Lecture Title");
        assertThat(dto.description).isEqualTo("Lecture Desc");
        assertThat(dto.serialNumber).isEqualTo(1);
        assertThat(dto.text).isEqualTo("Content body");
    }

    @Test
    void constructor_nullText_allowed() {
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
    void textConstant_hasExpectedValue() {
        assertThat(LectureDTO.TEXT).isEqualTo("text");
    }

    @Test
    void isSubclassOfCurriculumItemDTO() {
        // given
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2,
                LectureDTO.TEXT, 3
        );
        final Object[] tuples = {"T", "D", 1, "Text"};

        // when
        final LectureDTO dto = new LectureDTO(UUID_VALUE, tuples, aliasMap);

        // then
        assertThat(dto).isInstanceOf(CurriculumItemDTO.class);
    }
}
