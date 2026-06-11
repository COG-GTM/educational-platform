package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseReviewFactoryTest {

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
    void createFrom_validCommand_returnsCourseReview() {
        // given
        final UUID courseId = UUID.randomUUID();
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "great");

        final ReviewableCourse course = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(course, "id", 10);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 20);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final CourseReview result = sut.createFrom(command);

        // then
        assertThat(result).isNotNull();
        assertThat(result.toIdentifier()).isNotNull();
        assertThat(result)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("great"));
    }

    @Test
    void createFrom_invalidRating_constraintViolationException() {
        // given
        final UUID courseId = UUID.randomUUID();
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 6.0, "comment");

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }

    @Test
    void createFrom_negativeRating_constraintViolationException() {
        // given
        final UUID courseId = UUID.randomUUID();
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, -1.0, "comment");

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }

    @Test
    void createFrom_courseIdIsNull_constraintViolationException() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(null, 4.0, "comment");

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }

    @Test
    void createFrom_nullRating_constraintViolationException() {
        // given
        final UUID courseId = UUID.randomUUID();
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, null, "comment");

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }
}
