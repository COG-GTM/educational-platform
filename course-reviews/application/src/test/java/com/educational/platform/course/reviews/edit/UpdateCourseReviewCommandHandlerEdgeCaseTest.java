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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCourseReviewCommandHandlerEdgeCaseTest {

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
    void handle_updateRatingOnly_commentPreserved() {
        // given
        final UUID uuid = configureCourseReview(4.0, "original comment");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 2.0, "original comment");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        final CourseReview review = argument.getValue();
        assertThat(review)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(2.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("original comment"));
    }

    @Test
    void handle_updateCommentOnly_ratingPreserved() {
        // given
        final UUID uuid = configureCourseReview(4.0, "original comment");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 4.0, "new comment");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        final CourseReview review = argument.getValue();
        assertThat(review)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("new comment"));
    }

    @Test
    void handle_uuidIsNull_resourceNotFoundException() {
        // given
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(null, 3.0, "comment");
        when(courseReviewRepository.findByUuid(null)).thenReturn(Optional.empty());

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(com.educational.platform.common.exception.ResourceNotFoundException.class).isThrownBy(handle);
    }

    private UUID configureCourseReview(double rating, String comment) {
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        final ReviewCourseCommand reviewCourseCommand = new ReviewCourseCommand(courseId, rating, comment);
        final CourseReview courseReview = courseReviewFactory.createFrom(reviewCourseCommand);
        final UUID uuid = (UUID) ReflectionTestUtils.getField(courseReview, "uuid");
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.of(courseReview));
        return uuid;
    }
}
