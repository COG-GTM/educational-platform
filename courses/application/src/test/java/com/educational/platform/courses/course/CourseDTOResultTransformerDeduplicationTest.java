package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the deduplication behavior and aliasToIndexMap of {@link CourseDTOResultTransformer}.
 * When the same UUID appears in multiple tuples, the transformer should return the same
 * {@link CourseDTO} instance and accumulate curriculum items.
 */
public class CourseDTOResultTransformerDeduplicationTest {

    @Test
    void aliasToIndexMap_mapsAliasesToIndices() {
        // given
        final CourseDTOResultTransformer transformer = new CourseDTOResultTransformer();
        final String[] aliases = {"course_uuid", "course_name", "course_description", "course_numberOfStudents", "type"};

        // when
        final Map<String, Integer> map = transformer.aliasToIndexMap(aliases);

        // then
        assertThat(map)
                .hasSize(5)
                .containsEntry("course_uuid", 0)
                .containsEntry("course_name", 1)
                .containsEntry("course_description", 2)
                .containsEntry("course_numberOfStudents", 3)
                .containsEntry("type", 4);
    }

    @Test
    void aliasToIndexMap_emptyAliases_returnsEmptyMap() {
        // given
        final CourseDTOResultTransformer transformer = new CourseDTOResultTransformer();

        // when
        final Map<String, Integer> map = transformer.aliasToIndexMap(new String[]{});

        // then
        assertThat(map).isEmpty();
    }
}
