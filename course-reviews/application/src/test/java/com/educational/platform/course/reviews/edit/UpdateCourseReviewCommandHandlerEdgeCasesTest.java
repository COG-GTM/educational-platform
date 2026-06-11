package com.educational.platform.course.reviews.edit;

import com.educational.platform.course.reviews.*;
import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateCourseReviewCommandHandlerEdgeCasesTest {

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
    void handle_ratingExceedsMax_constraintViolationException() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 6.0, "comment");

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_negativeRating_constraintViolationException() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, -1.0, "comment");

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    private UUID configureCourseReview() {
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        final ReviewCourseCommand reviewCourseCommand = new ReviewCourseCommand(courseId, 4.0, "comment");
        final CourseReview courseReview = courseReviewFactory.createFrom(reviewCourseCommand);
        final UUID uuid = (UUID) ReflectionTestUtils.getField(courseReview, "uuid");
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.of(courseReview));
        return uuid;
    }
}
