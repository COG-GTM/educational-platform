package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class LectureDTOTest {

    @Test
    void constructor_validTuples_lectureDtoCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Map<String, Integer> aliasToIndexMap = new LinkedHashMap<>();
        aliasToIndexMap.put(CurriculumItemDTO.TITLE, 0);
        aliasToIndexMap.put(CurriculumItemDTO.DESCRIPTION, 1);
        aliasToIndexMap.put(CurriculumItemDTO.SERIAL_NUMBER, 2);
        aliasToIndexMap.put(LectureDTO.TEXT, 3);

        final Object[] tuples = new Object[]{"Lecture Title", "Lecture Desc", 1, "Lecture content text"};

        // when
        final LectureDTO sut = new LectureDTO(uuid, tuples, aliasToIndexMap);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("title", "Lecture Title")
                .hasFieldOrPropertyWithValue("description", "Lecture Desc")
                .hasFieldOrPropertyWithValue("serialNumber", 1)
                .hasFieldOrPropertyWithValue("text", "Lecture content text");
    }

    @Test
    void constructor_nullText_lectureDtoCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final Map<String, Integer> aliasToIndexMap = new LinkedHashMap<>();
        aliasToIndexMap.put(CurriculumItemDTO.TITLE, 0);
        aliasToIndexMap.put(CurriculumItemDTO.DESCRIPTION, 1);
        aliasToIndexMap.put(CurriculumItemDTO.SERIAL_NUMBER, 2);
        aliasToIndexMap.put(LectureDTO.TEXT, 3);

        final Object[] tuples = new Object[]{"Title", "Desc", 2, null};

        // when
        final LectureDTO sut = new LectureDTO(uuid, tuples, aliasToIndexMap);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("text", null)
                .hasFieldOrPropertyWithValue("serialNumber", 2);
    }
}
