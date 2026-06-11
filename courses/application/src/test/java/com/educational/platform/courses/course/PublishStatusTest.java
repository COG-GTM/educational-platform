package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PublishStatusTest {

    @Test
    void values_containsAllExpectedStatuses() {
        // then
        assertThat(PublishStatus.values()).containsExactlyInAnyOrder(
                PublishStatus.DRAFT,
                PublishStatus.PUBLISHED,
                PublishStatus.ARCHIVED
        );
    }

    @Test
    void values_hasThreeStatuses() {
        // then
        assertThat(PublishStatus.values()).hasSize(3);
    }

    @Test
    void valueOf_validName_returnsCorrectEnum() {
        // then
        assertThat(PublishStatus.valueOf("DRAFT")).isEqualTo(PublishStatus.DRAFT);
        assertThat(PublishStatus.valueOf("PUBLISHED")).isEqualTo(PublishStatus.PUBLISHED);
        assertThat(PublishStatus.valueOf("ARCHIVED")).isEqualTo(PublishStatus.ARCHIVED);
    }

    @Test
    void valueOf_invalidName_throwsIllegalArgumentException() {
        // then
        assertThatThrownBy(() -> PublishStatus.valueOf("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void name_returnsCorrectStringRepresentation() {
        // then
        assertThat(PublishStatus.DRAFT.name()).isEqualTo("DRAFT");
        assertThat(PublishStatus.PUBLISHED.name()).isEqualTo("PUBLISHED");
        assertThat(PublishStatus.ARCHIVED.name()).isEqualTo("ARCHIVED");
    }

    @Test
    void ordinal_valuesAreSequential() {
        // then
        assertThat(PublishStatus.DRAFT.ordinal()).isEqualTo(0);
        assertThat(PublishStatus.PUBLISHED.ordinal()).isEqualTo(1);
        assertThat(PublishStatus.ARCHIVED.ordinal()).isEqualTo(2);
    }
}
