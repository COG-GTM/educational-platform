package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseReviewUpdateTest {

    @Mock
    private ReviewableCourseRepository reviewableCourseRepository;

    @Mock
    private CurrentUserAsReviewer currentUserAsReviewer;

    private CourseReviewFactory factory;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        factory = new CourseReviewFactory(validator, currentUserAsReviewer, reviewableCourseRepository);
    }

    @Test
    void update_changesRatingAndComment() {
        // given
        final CourseReview review = createReview(4.0, "Initial comment");
        final UUID uuid = review.toIdentifier();

        // when
        review.update(new UpdateCourseReviewCommand(uuid, 2.5, "Updated comment"));

        // then
        assertThat(review)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(2.5))
                .hasFieldOrPropertyWithValue("comment", new Comment("Updated comment"));
    }

    @Test
    void update_preservesUuid() {
        // given
        final CourseReview review = createReview(4.0, "Initial");
        final UUID originalUuid = review.toIdentifier();

        // when
        review.update(new UpdateCourseReviewCommand(originalUuid, 3.0, "Changed"));

        // then
        assertThat(review.toIdentifier()).isEqualTo(originalUuid);
    }

    @Test
    void update_calledMultipleTimes_lastValuesWin() {
        // given
        final CourseReview review = createReview(4.0, "First");
        final UUID uuid = review.toIdentifier();

        // when
        review.update(new UpdateCourseReviewCommand(uuid, 3.0, "Second"));
        review.update(new UpdateCourseReviewCommand(uuid, 1.0, "Third"));

        // then
        assertThat(review)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(1.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("Third"));
    }

    @Test
    void toIdentifier_afterCreation_returnsNonNullUuid() {
        // given
        final CourseReview review = createReview(3.5, "Test");

        // then
        assertThat(review.toIdentifier()).isNotNull();
    }

    @Test
    void toIdentifier_twoReviews_haveDifferentUuids() {
        // given
        final CourseReview review1 = createReview(3.5, "Review 1");
        final CourseReview review2 = createReview(4.0, "Review 2");

        // then
        assertThat(review1.toIdentifier()).isNotEqualTo(review2.toIdentifier());
    }

    private CourseReview createReview(double rating, String comment) {
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewableCourse course = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("reviewer"));
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        return factory.createFrom(new ReviewCourseCommand(courseId, rating, comment));
    }
}
