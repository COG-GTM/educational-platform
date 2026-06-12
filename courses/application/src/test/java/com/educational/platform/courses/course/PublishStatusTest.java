package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PublishStatusTest {

    @Test
    void values_allStatusesExist() {
        // given
        final PublishStatus[] values = PublishStatus.values();

        // when / then
        assertThat(values).containsExactlyInAnyOrder(
                PublishStatus.DRAFT,
                PublishStatus.PUBLISHED,
                PublishStatus.ARCHIVED
        );
    }

    @Test
    void valueOf_draft_correctValue() {
        // when
        final PublishStatus status = PublishStatus.valueOf("DRAFT");

        // then
        assertThat(status).isEqualTo(PublishStatus.DRAFT);
    }

    @Test
    void valueOf_published_correctValue() {
        // when
        final PublishStatus status = PublishStatus.valueOf("PUBLISHED");

        // then
        assertThat(status).isEqualTo(PublishStatus.PUBLISHED);
    }

    @Test
    void valueOf_archived_correctValue() {
        // when
        final PublishStatus status = PublishStatus.valueOf("ARCHIVED");

        // then
        assertThat(status).isEqualTo(PublishStatus.ARCHIVED);
    }
}
