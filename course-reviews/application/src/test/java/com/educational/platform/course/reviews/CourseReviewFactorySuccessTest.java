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
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseReviewFactorySuccessTest {

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
    void createFrom_validCommand_reviewHasCorrectCourseAndReviewerReferences() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "Great course");

        final CreateReviewableCourseCommand createCourseCmd = new CreateReviewableCourseCommand(courseId);
        final ReviewableCourse course = new ReviewableCourse(createCourseCmd);
        ReflectionTestUtils.setField(course, "id", 42);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));

        final CreateReviewerCommand createReviewerCmd = new CreateReviewerCommand("reviewer-username");
        final Reviewer reviewer = new Reviewer(createReviewerCmd);
        ReflectionTestUtils.setField(reviewer, "id", 99);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final CourseReview review = sut.createFrom(command);

        // then
        assertThat(review)
                .hasFieldOrPropertyWithValue("course", 42)
                .hasFieldOrPropertyWithValue("reviewer", 99)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("Great course"));
        assertThat(review.toIdentifier()).isNotNull();
    }

    @Test
    void createFrom_twoCalls_differentUuids() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 3.5, "comment");

        final CreateReviewableCourseCommand createCourseCmd = new CreateReviewableCourseCommand(courseId);
        final ReviewableCourse course = new ReviewableCourse(createCourseCmd);
        ReflectionTestUtils.setField(course, "id", 42);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));

        final CreateReviewerCommand createReviewerCmd = new CreateReviewerCommand("reviewer-username");
        final Reviewer reviewer = new Reviewer(createReviewerCmd);
        ReflectionTestUtils.setField(reviewer, "id", 99);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final CourseReview review1 = sut.createFrom(command);
        final CourseReview review2 = sut.createFrom(command);

        // then
        assertThat(review1.toIdentifier()).isNotEqualTo(review2.toIdentifier());
    }
}
