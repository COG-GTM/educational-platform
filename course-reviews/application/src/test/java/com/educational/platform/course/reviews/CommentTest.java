package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CommentTest {

    @Test
    void constructor_validComment_commentCreated() {
        // given
        final String text = "Great course!";

        // when
        final Comment result = new Comment(text);

        // then
        assertThat(result.comment()).isEqualTo("Great course!");
    }

    @Test
    void constructor_nullComment_commentCreated() {
        // when
        final Comment result = new Comment(null);

        // then
        assertThat(result.comment()).isNull();
    }

    @Test
    void equals_sameComment_true() {
        // given
        final Comment comment1 = new Comment("text");
        final Comment comment2 = new Comment("text");

        // when/then
        assertThat(comment1).isEqualTo(comment2);
    }

    @Test
    void equals_differentComment_false() {
        // given
        final Comment comment1 = new Comment("text1");
        final Comment comment2 = new Comment("text2");

        // when/then
        assertThat(comment1).isNotEqualTo(comment2);
    }
}
