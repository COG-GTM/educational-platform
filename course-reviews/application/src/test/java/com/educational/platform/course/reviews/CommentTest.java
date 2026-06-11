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

    @Test
    void constructor_whitespaceOnlyComment_commentStoredAsIs() {
        // when — whitespace is not trimmed by the value object
        final Comment comment = new Comment("   ");

        // then
        assertThat(comment.comment()).isEqualTo("   ");
    }

    @Test
    void constructor_longComment_commentStoredInFull() {
        // when
        final String longText = "a".repeat(10_000);
        final Comment comment = new Comment(longText);

        // then
        assertThat(comment.comment()).hasSize(10_000).isEqualTo(longText);
    }

    @Test
    void implementsValueObject() {
        // then
        assertThat(new Comment("text")).isInstanceOf(com.educational.platform.common.domain.ValueObject.class);
    }

    @Test
    void equals_nullVsEmpty_returnsFalse() {
        // given
        final Comment nullComment = new Comment(null);
        final Comment emptyComment = new Comment("");

        // then
        assertThat(nullComment).isNotEqualTo(emptyComment);
    }
}
