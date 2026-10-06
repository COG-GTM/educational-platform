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
import com.educational.platform.course.reviews.rating.CourseRatingRecalculator;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReviewCourseCommandHandlerTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private ReviewableCourseRepository reviewableCourseRepository;

    @Mock
    private CurrentUserAsReviewer currentUserAsReviewer;

    @Mock
    private CourseRatingRecalculator courseRatingRecalculator;

    private ReviewCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        final CourseReviewFactory courseReviewFactory = new CourseReviewFactory(validator, currentUserAsReviewer, reviewableCourseRepository);
        sut = new ReviewCourseCommandHandler(courseReviewRepository, courseReviewFactory, courseRatingRecalculator);
    }

    @Test
    void handle_validCommand_reviewSavedAndCourseRatingRecalculated() {
        // given
        configureCourseAndReviewer();
        final ReviewCourseCommand command = new ReviewCourseCommand(COURSE_ID, 4.0, "comment");

        // when
        final UUID result = sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        final InOrder inOrder = inOrder(courseReviewRepository, courseRatingRecalculator);
        inOrder.verify(courseReviewRepository).save(argument.capture());
        inOrder.verify(courseRatingRecalculator).recalculate(COURSE_ID);
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("uuid", result)
                .hasFieldOrPropertyWithValue("course", 7)
                .hasFieldOrPropertyWithValue("reviewer", 11)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4))
                .hasFieldOrPropertyWithValue("comment", new Comment("comment"));
    }

    @Test
    void handle_unknownCourse_relatedResourceIsNotResolvedExceptionAndNothingSavedOrRecalculated() {
        // given
        when(reviewableCourseRepository.findByOriginalCourseId(COURSE_ID)).thenReturn(Optional.empty());
        final ReviewCourseCommand command = new ReviewCourseCommand(COURSE_ID, 4.0, "comment");

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class).isThrownBy(handle);
        verifyNoInteractions(courseReviewRepository, courseRatingRecalculator);
    }

    @Test
    void handle_ratingAboveMaximum_constraintViolationExceptionAndNothingSavedOrRecalculated() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(COURSE_ID, 5.5, "comment");

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verifyNoInteractions(courseReviewRepository, courseRatingRecalculator);
    }

    @Test
    void handle_ratingEmpty_constraintViolationExceptionAndNothingSavedOrRecalculated() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(COURSE_ID, null, "comment");

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verifyNoInteractions(courseReviewRepository, courseRatingRecalculator);
    }

    private void configureCourseAndReviewer() {
        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(COURSE_ID));
        ReflectionTestUtils.setField(reviewableCourse, "id", 7);
        when(reviewableCourseRepository.findByOriginalCourseId(COURSE_ID)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 11);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);
    }
}
