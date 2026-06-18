package com.educational.platform.course.reviews.create;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.reviews.Comment;
import com.educational.platform.course.reviews.CourseRating;
import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewFactory;
import com.educational.platform.course.reviews.CourseReviewRepository;
import com.educational.platform.course.reviews.CurrentUserAsReviewer;
import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReviewCourseCommandHandlerTest {

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private ReviewableCourseRepository reviewableCourseRepository;

    @Mock
    private CurrentUserAsReviewer currentUserAsReviewer;

    private ReviewCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        final CourseReviewFactory courseReviewFactory =
                new CourseReviewFactory(validator, currentUserAsReviewer, reviewableCourseRepository);
        sut = new ReviewCourseCommandHandler(courseReviewRepository, courseReviewFactory);
    }

    @Test
    void handle_validCommand_reviewSavedAndIdentifierReturned() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        configureResolvableReviewer(courseId);
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "great course");

        // when
        final UUID result = sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        final CourseReview savedReview = argument.getValue();
        assertThat(savedReview)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("great course"));
        assertThat(result).isEqualTo(savedReview.toIdentifier());
    }

    @Test
    void handle_courseCannotBeResolved_relatedResourceIsNotResolvedExceptionAndNoSave() {
        // given - the reviewable-course projection is missing, so the course relation cannot be resolved
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.empty());
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "great course");

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class).isThrownBy(handle);
        verify(courseReviewRepository, never()).save(any());
    }

    @Test
    void handle_ratingAboveMaximum_constraintViolationExceptionAndNoSave() {
        // given - rating 6.0 violates the @Max(5) bound, the review must never be persisted
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 6.0, "great course");

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verify(courseReviewRepository, never()).save(any());
    }

    @Test
    void handle_nullRating_constraintViolationExceptionAndNoSave() {
        // given - rating is @NotNull, an empty rating must never be persisted
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440004");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, null, "great course");

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verify(courseReviewRepository, never()).save(any());
    }

    @Test
    void handle_repositoryThrows_exceptionPropagated() {
        // given - a persistence failure must propagate so the transactional handler rolls back
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440005");
        configureResolvableReviewer(courseId);
        doThrow(new RuntimeException("review could not be saved"))
                .when(courseReviewRepository).save(any(CourseReview.class));
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "great course");

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(handle)
                .withMessage("review could not be saved");
    }

    private void configureResolvableReviewer(UUID courseId) {
        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("reviewer"));
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);
    }
}
