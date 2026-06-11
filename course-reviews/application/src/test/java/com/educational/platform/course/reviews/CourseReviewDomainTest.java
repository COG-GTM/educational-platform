package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseReviewDomainTest {

    @Test
    void constructor_initializesFieldsFromCommand() {
        // given
        final UUID courseId = UUID.randomUUID();
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.5, "Great course");

        // when
        final CourseReview review = new CourseReview(command, 10, 20);

        // then
        assertThat(review.toIdentifier()).isNotNull();
        assertThat(review)
                .hasFieldOrPropertyWithValue("course", 10)
                .hasFieldOrPropertyWithValue("reviewer", 20)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.5))
                .hasFieldOrPropertyWithValue("comment", new Comment("Great course"));
    }

    @Test
    void constructor_generatesUniqueUuids() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(UUID.randomUUID(), 3.0, "OK");

        // when
        final CourseReview review1 = new CourseReview(command, 1, 1);
        final CourseReview review2 = new CourseReview(command, 1, 1);

        // then
        assertThat(review1.toIdentifier()).isNotEqualTo(review2.toIdentifier());
    }

    @Test
    void update_changesRatingAndComment() {
        // given
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(UUID.randomUUID(), 3.0, "OK");
        final CourseReview review = new CourseReview(createCommand, 1, 1);
        final UUID originalUuid = review.toIdentifier();

        // when
        final UpdateCourseReviewCommand updateCommand = new UpdateCourseReviewCommand(originalUuid, 5.0, "Excellent!");
        review.update(updateCommand);

        // then
        assertThat(review)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("Excellent!"));
        assertThat(review.toIdentifier()).isEqualTo(originalUuid);
    }

    @Test
    void update_withNullComment_setsNullComment() {
        // given
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(UUID.randomUUID(), 4.0, "Good");
        final CourseReview review = new CourseReview(createCommand, 1, 1);

        // when
        final UpdateCourseReviewCommand updateCommand = new UpdateCourseReviewCommand(review.toIdentifier(), 2.0, null);
        review.update(updateCommand);

        // then
        assertThat(review)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(2.0))
                .hasFieldOrPropertyWithValue("comment", new Comment(null));
    }

    @Test
    void update_preservesCourseAndReviewer() {
        // given
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(UUID.randomUUID(), 3.0, "OK");
        final CourseReview review = new CourseReview(createCommand, 42, 99);

        // when
        final UpdateCourseReviewCommand updateCommand = new UpdateCourseReviewCommand(review.toIdentifier(), 1.0, "Bad");
        review.update(updateCommand);

        // then
        assertThat(review)
                .hasFieldOrPropertyWithValue("course", 42)
                .hasFieldOrPropertyWithValue("reviewer", 99);
    }
}
