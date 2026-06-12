package com.educational.platform.course.reviews.create;

import com.educational.platform.course.reviews.*;
import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
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

@ExtendWith(MockitoExtension.class)
class ReviewCourseCommandHandlerSuccessTest {

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
        final CourseReviewFactory factory = new CourseReviewFactory(validator, currentUserAsReviewer, reviewableCourseRepository);
        sut = new ReviewCourseCommandHandler(courseReviewRepository, factory);
    }

    @Test
    void handle_validCommand_reviewSavedWithCorrectFields() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.5, "Excellent course");

        final CreateReviewableCourseCommand createCourseCmd = new CreateReviewableCourseCommand(courseId);
        final ReviewableCourse course = new ReviewableCourse(createCourseCmd);
        ReflectionTestUtils.setField(course, "id", 42);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));

        final CreateReviewerCommand createReviewerCmd = new CreateReviewerCommand("reviewer");
        final Reviewer reviewer = new Reviewer(createReviewerCmd);
        ReflectionTestUtils.setField(reviewer, "id", 99);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
        final ArgumentCaptor<CourseReview> captor = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(captor.capture());
        final CourseReview savedReview = captor.getValue();
        assertThat(savedReview)
                .hasFieldOrPropertyWithValue("course", 42)
                .hasFieldOrPropertyWithValue("reviewer", 99)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.5))
                .hasFieldOrPropertyWithValue("comment", new Comment("Excellent course"));
        assertThat(savedReview.toIdentifier()).isEqualTo(result);
    }

    @Test
    void handle_twoReviews_differentUuidsReturned() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 3.0, "OK");

        final CreateReviewableCourseCommand createCourseCmd = new CreateReviewableCourseCommand(courseId);
        final ReviewableCourse course = new ReviewableCourse(createCourseCmd);
        ReflectionTestUtils.setField(course, "id", 42);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));

        final CreateReviewerCommand createReviewerCmd = new CreateReviewerCommand("reviewer");
        final Reviewer reviewer = new Reviewer(createReviewerCmd);
        ReflectionTestUtils.setField(reviewer, "id", 99);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final UUID uuid1 = sut.handle(command);
        final UUID uuid2 = sut.handle(command);

        // then
        assertThat(uuid1).isNotEqualTo(uuid2);
    }

    @Test
    void handle_boundaryRatingZero_reviewCreatedSuccessfully() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 0.0, "Poor course");

        final CreateReviewableCourseCommand createCourseCmd = new CreateReviewableCourseCommand(courseId);
        final ReviewableCourse course = new ReviewableCourse(createCourseCmd);
        ReflectionTestUtils.setField(course, "id", 10);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));

        final CreateReviewerCommand createReviewerCmd = new CreateReviewerCommand("reviewer");
        final Reviewer reviewer = new Reviewer(createReviewerCmd);
        ReflectionTestUtils.setField(reviewer, "id", 5);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
        final ArgumentCaptor<CourseReview> captor = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }

    @Test
    void handle_boundaryRatingFive_reviewCreatedSuccessfully() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 5.0, "Perfect course");

        final CreateReviewableCourseCommand createCourseCmd = new CreateReviewableCourseCommand(courseId);
        final ReviewableCourse course = new ReviewableCourse(createCourseCmd);
        ReflectionTestUtils.setField(course, "id", 10);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));

        final CreateReviewerCommand createReviewerCmd = new CreateReviewerCommand("reviewer");
        final Reviewer reviewer = new Reviewer(createReviewerCmd);
        ReflectionTestUtils.setField(reviewer, "id", 5);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
        final ArgumentCaptor<CourseReview> captor = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0));
    }
}
