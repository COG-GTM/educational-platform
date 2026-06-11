package com.educational.platform.course.reviews.edit;

import com.educational.platform.common.exception.ResourceNotFoundException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateCourseReviewCommandHandlerTest {

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
    void handle_existingCourseReview_reviewSavedWithUpdatedFields() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 3.0, "updated comment");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        final CourseReview review = argument.getValue();
        assertThat(review)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(3))
                .hasFieldOrPropertyWithValue("comment", new Comment("updated comment"));
    }

    @Test
    void handle_invalidId_resourceNotFoundException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 3.0, "updated comment");
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
    }

    @Test
    void handle_ratingEmpty_resourceNotFoundException() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, null, "updated comment");

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, 6})
    void handle_invalidRating_constraintViolationException(double rating) {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, rating, "comment");

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_validUpdate_uuidPreserved() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 2.0, "changed");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void handle_uuidEmpty_resourceNotFoundException() {
        // given
        final UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 3.0, "comment");
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
    }

    @Test
    void handle_validUpdate_courseAndReviewerUnchanged() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(reviewableCourse, "id", 11);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 22);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        final ReviewCourseCommand reviewCourseCommand = new ReviewCourseCommand(courseId, 4.0, "comment");
        final CourseReview courseReview = courseReviewFactory.createFrom(reviewCourseCommand);
        final UUID uuid = (UUID) ReflectionTestUtils.getField(courseReview, "uuid");
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.of(courseReview));

        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 2.0, "changed");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("course", 11)
                .hasFieldOrPropertyWithValue("reviewer", 22);
    }

    @Test
    void handle_nullComment_reviewSaved() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 3.0, null);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("comment", new Comment(null));
    }

    @Test
    void handle_emptyComment_reviewSaved() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 3.0, "");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("comment", new Comment(""));
    }

    @Test
    void handle_maxRating_reviewSaved() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 5.0, "max");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0));
    }

    @Test
    void handle_zeroRating_reviewSaved() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 0.0, "zero");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }

    @Test
    void handle_existingReviewInvalidRating_constraintViolationException() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 5.1, "comment");

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_validUpdate_saveCalledExactlyOnce() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 3.0, "comment");

        // when
        sut.handle(command);

        // then
        verify(courseReviewRepository, org.mockito.Mockito.times(1)).save(org.mockito.ArgumentMatchers.any(CourseReview.class));
    }

    @Test
    void handle_notFoundWithInvalidRating_resourceNotFoundExceptionTakesPrecedence() {
        // given — UUID not found AND rating is also invalid (-1); not-found check runs first
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440099");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, -1.0, "comment");
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
    }

    @Test
    void handle_existingReviewNullUuid_resourceNotFoundException() {
        // given
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(null, 3.0, "comment");
        when(courseReviewRepository.findByUuid(null)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
    }

    @Test
    void handle_validationFails_saveNotCalled() {
        // given — review found but rating is null (constraint violation); save must not be called
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, null, "comment");

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verify(courseReviewRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any(CourseReview.class));
    }

    @Test
    void handle_validUpdate_findByUuidCalledWithCorrectUuid() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 3.0, "comment");

        // when
        sut.handle(command);

        // then
        verify(courseReviewRepository).findByUuid(uuid);
    }

    @Test
    void handle_validUpdate_ratingAndCommentBothUpdated() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 1.5, "half-star");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("rating", new CourseRating(1.5))
                .hasFieldOrPropertyWithValue("comment", new Comment("half-star"));
    }

    @Test
    void handle_twoSequentialUpdates_lastStateApplied() {
        // given
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand firstUpdate = new UpdateCourseReviewCommand(uuid, 1.0, "first");
        final UpdateCourseReviewCommand secondUpdate = new UpdateCourseReviewCommand(uuid, 5.0, "second");

        // when
        sut.handle(firstUpdate);
        sut.handle(secondUpdate);

        // then
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository, org.mockito.Mockito.times(2)).save(argument.capture());
        final CourseReview lastSaved = argument.getAllValues().get(1);
        assertThat(lastSaved)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("second"));
    }

    private UUID configureCourseReview() {
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final String reviewerUsername = "username";
        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand(reviewerUsername));
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        final ReviewCourseCommand reviewCourseCommand = new ReviewCourseCommand(courseId, 4.0, "comment");
        final CourseReview courseReview = courseReviewFactory.createFrom(reviewCourseCommand);
        final UUID uuid = (UUID) ReflectionTestUtils.getField(courseReview, "uuid");
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.of(courseReview));
        return uuid;
    }
}
