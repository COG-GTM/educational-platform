package com.educational.platform.course.reviews.edit;

import com.educational.platform.course.reviews.*;
import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link UpdateCourseReviewCommandHandler} handles null and
 * empty comment values correctly when updating an existing review.
 */
@ExtendWith(MockitoExtension.class)
public class UpdateCourseReviewWithNullCommentTest {

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private ReviewableCourseRepository reviewableCourseRepository;

    @Mock
    private CurrentUserAsReviewer currentUserAsReviewer;

    private CourseReviewFactory courseReviewFactory;
    private UpdateCourseReviewCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        courseReviewFactory = new CourseReviewFactory(validator, currentUserAsReviewer, reviewableCourseRepository);
        sut = new UpdateCourseReviewCommandHandler(validator, courseReviewRepository);
    }

    @Test
    void handle_nullComment_updatesReviewWithNullComment() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 2.0, null);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> captor = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("rating", new CourseRating(2.0))
                .hasFieldOrPropertyWithValue("comment", new Comment(null));
    }

    @Test
    void handle_emptyComment_updatesReviewWithEmptyComment() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 4.0, "");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> captor = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.0))
                .hasFieldOrPropertyWithValue("comment", new Comment(""));
    }

    @Test
    void handle_boundaryRatingZero_updatesSuccessfully() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 0.0, "comment");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> captor = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }

    @Test
    void handle_boundaryRatingMax_updatesSuccessfully() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 5.0, "max rating");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> captor = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0));
    }

    private UUID configureCourseReview() {
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewableCourse course = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("user"));
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        final CourseReview review = courseReviewFactory.createFrom(new ReviewCourseCommand(courseId, 4.0, "comment"));
        final UUID uuid = (UUID) ReflectionTestUtils.getField(review, "uuid");
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.of(review));
        return uuid;
    }
}
