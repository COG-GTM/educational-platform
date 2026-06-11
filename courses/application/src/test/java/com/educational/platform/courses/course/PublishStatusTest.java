package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PublishStatusTest {

    @Test
    void values_containsAllStatuses() {
        assertThat(PublishStatus.values()).containsExactly(
                PublishStatus.DRAFT,
                PublishStatus.PUBLISHED,
                PublishStatus.ARCHIVED);
    }

    @Test
    void valueOf_draft_returnsDraft() {
        assertThat(PublishStatus.valueOf("DRAFT")).isEqualTo(PublishStatus.DRAFT);
    }

    @Test
    void valueOf_published_returnsPublished() {
        assertThat(PublishStatus.valueOf("PUBLISHED")).isEqualTo(PublishStatus.PUBLISHED);
    }

    @Test
    void valueOf_archived_returnsArchived() {
        assertThat(PublishStatus.valueOf("ARCHIVED")).isEqualTo(PublishStatus.ARCHIVED);
    }

    @Test
    void valueOf_invalid_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> PublishStatus.valueOf("DELETED"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void valuesCount() {
        assertThat(PublishStatus.values()).hasSize(3);
    }
}
