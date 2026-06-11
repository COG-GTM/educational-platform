package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewValueObjectsBoundaryTest {

    @Test
    void comment_preservesText() {
        final Comment comment = new Comment("Great course!");
        assertThat(comment.comment()).isEqualTo("Great course!");
    }

    @Test
    void comment_nullValue_allowed() {
        final Comment comment = new Comment(null);
        assertThat(comment.comment()).isNull();
    }

    @Test
    void comment_emptyString_allowed() {
        final Comment comment = new Comment("");
        assertThat(comment.comment()).isEmpty();
    }

    @Test
    void comment_equalInstances() {
        assertThat(new Comment("text")).isEqualTo(new Comment("text"));
    }

    @Test
    void comment_differentText_notEqual() {
        assertThat(new Comment("abc")).isNotEqualTo(new Comment("xyz"));
    }

    @Test
    void comment_hashCodeConsistent() {
        assertThat(new Comment("text").hashCode())
                .isEqualTo(new Comment("text").hashCode());
    }

    @Test
    void courseRating_preservesValue() {
        final CourseRating rating = new CourseRating(4.5);
        assertThat(rating.rating()).isEqualTo(4.5);
    }

    @Test
    void courseRating_zeroAllowed() {
        final CourseRating rating = new CourseRating(0.0);
        assertThat(rating.rating()).isZero();
    }

    @Test
    void courseRating_maxBoundary() {
        final CourseRating rating = new CourseRating(5.0);
        assertThat(rating.rating()).isEqualTo(5.0);
    }

    @Test
    void courseRating_equalInstances() {
        assertThat(new CourseRating(3.0)).isEqualTo(new CourseRating(3.0));
    }

    @Test
    void courseRating_differentValues_notEqual() {
        assertThat(new CourseRating(3.0)).isNotEqualTo(new CourseRating(4.0));
    }

    @Test
    void courseRating_hashCodeConsistent() {
        assertThat(new CourseRating(3.5).hashCode())
                .isEqualTo(new CourseRating(3.5).hashCode());
    }
}
