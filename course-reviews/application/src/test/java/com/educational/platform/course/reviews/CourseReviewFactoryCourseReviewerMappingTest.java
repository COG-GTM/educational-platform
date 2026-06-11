package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
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
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link CourseReviewFactory} correctly maps the resolved
 * course and reviewer IDs into the created {@link CourseReview}.
 */
@ExtendWith(MockitoExtension.class)
public class CourseReviewFactoryCourseReviewerMappingTest {

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
    void createFrom_validCommand_courseIdMappedCorrectly() {
        // given
        final UUID courseId = UUID.randomUUID();
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 3.5, "good");

        final ReviewableCourse course = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(course, "id", 42);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("reviewer"));
        ReflectionTestUtils.setField(reviewer, "id", 99);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final CourseReview result = sut.createFrom(command);

        // then
        assertThat(result)
                .hasFieldOrPropertyWithValue("course", 42)
                .hasFieldOrPropertyWithValue("reviewer", 99);
    }

    @Test
    void createFrom_withNullComment_createsReviewWithNullComment() {
        // given
        final UUID courseId = UUID.randomUUID();
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 5.0, null);

        final ReviewableCourse course = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(course, "id", 10);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("user1"));
        ReflectionTestUtils.setField(reviewer, "id", 20);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final CourseReview result = sut.createFrom(command);

        // then
        assertThat(result)
                .hasFieldOrPropertyWithValue("comment", new Comment(null))
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0));
    }

    @Test
    void createFrom_withEmptyComment_createsReviewWithEmptyComment() {
        // given
        final UUID courseId = UUID.randomUUID();
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 1.0, "");

        final ReviewableCourse course = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(course, "id", 10);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("user2"));
        ReflectionTestUtils.setField(reviewer, "id", 20);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final CourseReview result = sut.createFrom(command);

        // then
        assertThat(result)
                .hasFieldOrPropertyWithValue("comment", new Comment(""))
                .hasFieldOrPropertyWithValue("rating", new CourseRating(1.0));
    }
}
