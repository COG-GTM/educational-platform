package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CommentTest {

    @Test
    void constructor_value_commentCreated() {
        // given
        final String value = "comment";

        // when
        final Comment comment = new Comment(value);

        // then
        assertThat(comment.comment()).isEqualTo("comment");
    }

    @Test
    void equals_sameValue_equal() {
        // given
        final Comment comment = new Comment("comment");

        // when
        final Comment same = new Comment("comment");

        // then
        assertThat(comment).isEqualTo(same);
        assertThat(comment).isNotEqualTo(new Comment("other"));
    }
}
