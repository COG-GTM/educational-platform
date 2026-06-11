package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseReviewFactorySuccessTest {

    @Mock
    private ReviewableCourseRepository reviewableCourseRepository;

    @Mock
    private CurrentUserAsReviewer currentUserAsReviewer;

    private CourseReviewFactory sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new CourseReviewFactory(validator, currentUserAsReviewer, reviewableCourseRepository);
    }

    @Test
    void createFrom_validCommand_createsCourseReviewWithUuid() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewableCourse course = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("reviewer1"));
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "Great course");

        // when
        final CourseReview result = sut.createFrom(command);

        // then
        assertThat(result.toIdentifier()).isNotNull();
    }

    @Test
    void createFrom_ratingExceedsMax_constraintViolationException() {
        // given
        final UUID courseId = UUID.randomUUID();
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 6.0, "Too high");

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }

    @Test
    void createFrom_negativeRating_constraintViolationException() {
        // given
        final UUID courseId = UUID.randomUUID();
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, -1.0, "Negative");

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }

    @Test
    void createFrom_zeroRating_createsCourseReview() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewableCourse course = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("reviewer1"));
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 0.0, "Zero rating");

        // when
        final CourseReview result = sut.createFrom(command);

        // then
        assertThat(result.toIdentifier()).isNotNull();
    }

    @Test
    void createFrom_maxRating_createsCourseReview() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewableCourse course = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("reviewer1"));
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 5.0, "Perfect");

        // when
        final CourseReview result = sut.createFrom(command);

        // then
        assertThat(result.toIdentifier()).isNotNull();
    }
}
