package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseDTOResultTransformerAliasTest {

    private final CourseDTOResultTransformer sut = new CourseDTOResultTransformer();

    @Test
    void aliasToIndexMap_mapsAliasesToIndices() {
        // given
        final String[] aliases = {"alias_a", "alias_b", "alias_c"};

        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(aliases);

        // then
        assertThat(result).hasSize(3)
                .containsEntry("alias_a", 0)
                .containsEntry("alias_b", 1)
                .containsEntry("alias_c", 2);
    }

    @Test
    void aliasToIndexMap_emptyAliases_returnsEmptyMap() {
        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(new String[]{});

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void aliasToIndexMap_singleAlias_mapsToZero() {
        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(new String[]{"only"});

        // then
        assertThat(result).hasSize(1).containsEntry("only", 0);
    }

    @Test
    void aliasToIndexMap_preservesInsertionOrder() {
        // given
        final String[] aliases = {"z", "a", "m"};

        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(aliases);

        // then
        var keys = result.keySet().stream().toList();
        assertThat(keys).containsExactly("z", "a", "m");
    }
}
