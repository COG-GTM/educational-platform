package com.educational.platform.course.reviews;

import com.educational.platform.common.domain.ValueObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests value objects in the course-reviews module: {@link Comment} and {@link CourseRating}.
 */
public class CourseReviewValueObjectsTest {

    // --- Comment ---

    @Test
    void comment_exposesComment() {
        final Comment comment = new Comment("Great course");
        assertThat(comment.comment()).isEqualTo("Great course");
    }

    @Test
    void comment_equalInstances() {
        assertThat(new Comment("text")).isEqualTo(new Comment("text"));
    }

    @Test
    void comment_differentValues_notEqual() {
        assertThat(new Comment("first")).isNotEqualTo(new Comment("second"));
    }

    @Test
    void comment_nullValue_allowed() {
        final Comment comment = new Comment(null);
        assertThat(comment.comment()).isNull();
    }

    @Test
    void comment_implementsValueObject() {
        assertThat(new Comment("text")).isInstanceOf(ValueObject.class);
    }

    @Test
    void comment_hashCodeConsistentWithEquals() {
        final Comment first = new Comment("same");
        final Comment second = new Comment("same");
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    // --- CourseRating ---

    @Test
    void courseRating_exposesRating() {
        final CourseRating rating = new CourseRating(4.5);
        assertThat(rating.rating()).isEqualTo(4.5);
    }

    @Test
    void courseRating_equalInstances() {
        assertThat(new CourseRating(3.0)).isEqualTo(new CourseRating(3.0));
    }

    @Test
    void courseRating_differentValues_notEqual() {
        assertThat(new CourseRating(4.0)).isNotEqualTo(new CourseRating(2.0));
    }

    @Test
    void courseRating_implementsValueObject() {
        assertThat(new CourseRating(1.0)).isInstanceOf(ValueObject.class);
    }

    @Test
    void courseRating_zeroRating() {
        assertThat(new CourseRating(0.0).rating()).isEqualTo(0.0);
    }

    @Test
    void courseRating_maxRating() {
        assertThat(new CourseRating(5.0).rating()).isEqualTo(5.0);
    }
}
