package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseReviewEdgeCaseTest {

    @Test
    void toIdentifier_afterConstruction_returnsNonNullUuid() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(UUID.randomUUID(), 4.0, "Great course");
        final CourseReview sut = new CourseReview(command, 1, 2);

        // when
        final UUID result = sut.toIdentifier();

        // then
        assertThat(result).isNotNull();
    }

    @Test
    void toIdentifier_stableAcrossMultipleCalls() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(UUID.randomUUID(), 4.0, "Great course");
        final CourseReview sut = new CourseReview(command, 1, 2);

        // when
        final UUID first = sut.toIdentifier();
        final UUID second = sut.toIdentifier();

        // then
        assertThat(first).isEqualTo(second);
    }

    @Test
    void update_preservesUuid() {
        // given
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(UUID.randomUUID(), 4.0, "Good");
        final CourseReview sut = new CourseReview(createCommand, 1, 2);
        final UUID originalUuid = sut.toIdentifier();

        // when
        final UpdateCourseReviewCommand updateCommand = new UpdateCourseReviewCommand(originalUuid, 3.0, "Updated");
        sut.update(updateCommand);

        // then
        assertThat(sut.toIdentifier()).isEqualTo(originalUuid);
    }

    @Test
    void update_changesRatingAndComment() {
        // given
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(UUID.randomUUID(), 4.0, "Good");
        final CourseReview sut = new CourseReview(createCommand, 1, 2);

        // when
        final UpdateCourseReviewCommand updateCommand = new UpdateCourseReviewCommand(sut.toIdentifier(), 2.0, "Not so good");
        sut.update(updateCommand);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(2.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("Not so good"));
    }

    @Test
    void update_calledTwice_lastUpdateWins() {
        // given
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(UUID.randomUUID(), 4.0, "Good");
        final CourseReview sut = new CourseReview(createCommand, 1, 2);

        // when
        sut.update(new UpdateCourseReviewCommand(sut.toIdentifier(), 3.0, "OK"));
        sut.update(new UpdateCourseReviewCommand(sut.toIdentifier(), 5.0, "Excellent"));

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("Excellent"));
    }

    @Test
    void constructor_twoReviews_differentUuids() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(UUID.randomUUID(), 4.0, "Good");

        // when
        final CourseReview review1 = new CourseReview(command, 1, 2);
        final CourseReview review2 = new CourseReview(command, 1, 2);

        // then
        assertThat(review1.toIdentifier()).isNotEqualTo(review2.toIdentifier());
    }

    @Test
    void constructor_preservesCourseAndReviewerReferences() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(UUID.randomUUID(), 3.5, "Nice");

        // when
        final CourseReview sut = new CourseReview(command, 42, 99);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("course", 42)
                .hasFieldOrPropertyWithValue("reviewer", 99)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(3.5))
                .hasFieldOrPropertyWithValue("comment", new Comment("Nice"));
    }
}
