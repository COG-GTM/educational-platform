package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class LectureDTOTest {

    @Test
    void tupleConstructor_mapsBaseFieldsAndText() {
        // given - a lecture row is mapped from the joined tuple via the alias -> index map; the aliases are
        // out of order to prove the lookup is index-driven, and the course uuid is supplied separately by the
        // transformer (it owns the parent CourseDTO key) rather than read from the curriculum-item columns
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuple = {
                42,
                "lecture body",
                "Lecture Title",
                "Lecture Description"
        };
        final Map<String, Integer> aliasToIndexMap = Map.of(
                CurriculumItemDTO.SERIAL_NUMBER, 0,
                LectureDTO.TEXT, 1,
                CurriculumItemDTO.TITLE, 2,
                CurriculumItemDTO.DESCRIPTION, 3);

        // when
        final LectureDTO dto = new LectureDTO(courseUuid, tuple, aliasToIndexMap);

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("uuid", courseUuid)
                .hasFieldOrPropertyWithValue("title", "Lecture Title")
                .hasFieldOrPropertyWithValue("description", "Lecture Description")
                .hasFieldOrPropertyWithValue("serialNumber", 42)
                .hasFieldOrPropertyWithValue("text", "lecture body");
    }
}
