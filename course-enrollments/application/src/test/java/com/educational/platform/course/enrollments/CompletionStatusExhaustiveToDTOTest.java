package com.educational.platform.course.enrollments;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parametrized test ensuring every {@link CompletionStatus} enum value maps
 * to a non-null {@link CompletionStatusDTO} with a matching name.
 * Guards against new enum values being added without a corresponding DTO mapping.
 */
public class CompletionStatusExhaustiveToDTOTest {

    @ParameterizedTest
    @EnumSource(CompletionStatus.class)
    void toDTO_allValues_returnNonNullDTOWithMatchingName(CompletionStatus status) {
        // when
        final CompletionStatusDTO dto = status.toDTO();

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.name()).isEqualTo(status.name());
    }
}
