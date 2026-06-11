package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CommentTest {

    @Test
    void constructor_validComment_commentStored() {
        // when
        final Comment comment = new Comment("great course");

        // then
        assertThat(comment.comment()).isEqualTo("great course");
    }

    @Test
    void equals_sameComment_returnsTrue() {
        // given
        final Comment first = new Comment("comment");
        final Comment second = new Comment("comment");

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_differentComment_returnsFalse() {
        // given
        final Comment first = new Comment("comment1");
        final Comment second = new Comment("comment2");

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void constructor_nullComment_commentStoredAsNull() {
        // when
        final Comment comment = new Comment(null);

        // then
        assertThat(comment.comment()).isNull();
    }

    @Test
    void constructor_emptyComment_commentStoredAsEmpty() {
        // when
        final Comment comment = new Comment("");

        // then
        assertThat(comment.comment()).isEmpty();
    }

    @Test
    void equals_bothNull_returnsTrue() {
        // given
        final Comment first = new Comment(null);
        final Comment second = new Comment(null);

        // then
        assertThat(first).isEqualTo(second);
    }
}
