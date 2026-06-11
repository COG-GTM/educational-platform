package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewsValueObjectsTest {

    @Test
    void comment_exposesValueAndEquality() {
        assertThat(new Comment("text").comment()).isEqualTo("text");
        assertThat(new Comment("text")).isEqualTo(new Comment("text"));
        assertThat(new Comment("text")).isNotEqualTo(new Comment("other"));
    }

    @Test
    void courseRating_exposesValueAndEquality() {
        assertThat(new CourseRating(4.5).rating()).isEqualTo(4.5);
        assertThat(new CourseRating(4.5)).isEqualTo(new CourseRating(4.5));
        assertThat(new CourseRating(4.5)).isNotEqualTo(new CourseRating(1.0));
    }
}
