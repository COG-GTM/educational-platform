package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseDTOResultTransformerTest {

    private final CourseDTOResultTransformer sut = new CourseDTOResultTransformer();

    @Test
    void aliasToIndexMap_multipleAliases_correctMapping() {
        // given
        final String[] aliases = {"col_a", "col_b", "col_c"};

        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(aliases);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get("col_a")).isEqualTo(0);
        assertThat(result.get("col_b")).isEqualTo(1);
        assertThat(result.get("col_c")).isEqualTo(2);
    }

    @Test
    void aliasToIndexMap_emptyAliases_emptyMap() {
        // given
        final String[] aliases = {};

        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(aliases);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void aliasToIndexMap_singleAlias_correctMapping() {
        // given
        final String[] aliases = {"uuid"};

        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(aliases);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get("uuid")).isEqualTo(0);
    }

    @Test
    void aliasToIndexMap_preservesInsertionOrder() {
        // given
        final String[] aliases = {CourseDTO.UUID_COLUMN, CourseDTO.NAME_COLUMN, CourseDTO.DESCRIPTION_COLUMN};

        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(aliases);

        // then
        assertThat(result.keySet()).containsExactly(
                CourseDTO.UUID_COLUMN, CourseDTO.NAME_COLUMN, CourseDTO.DESCRIPTION_COLUMN
        );
    }
}
