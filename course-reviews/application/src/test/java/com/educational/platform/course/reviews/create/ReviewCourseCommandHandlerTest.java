package com.educational.platform.course.reviews.create;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
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
        final CourseReviewFactory courseReviewFactory = new CourseReviewFactory(validator, currentUserAsReviewer, reviewableCourseRepository);
        sut = new ReviewCourseCommandHandler(courseReviewRepository, courseReviewFactory);
    }

    @Test
    void handle_validCommand_courseReviewSavedAndUUIDReturned() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(reviewableCourse, "id", 11);
        ReflectionTestUtils.setField(reviewableCourse, "originalCourseId", courseId);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 22);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        final CourseReview savedReview = argument.getValue();
        assertThat(savedReview)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4))
                .hasFieldOrPropertyWithValue("comment", new Comment("comment"));
    }

    @Test
    void handle_invalidRating_constraintViolationException() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, -1.0, "comment");

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_nullRating_constraintViolationException() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, null, "comment");

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_courseNotFound_relatedResourceIsNotResolvedException() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.empty());

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class).isThrownBy(handle);
    }

    @Test
    void handle_validCommand_returnedUuidMatchesSavedEntity() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(reviewableCourse, "id", 11);
        ReflectionTestUtils.setField(reviewableCourse, "originalCourseId", courseId);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 22);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final UUID result = sut.handle(command);

        // then
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        assertThat(result).isEqualTo(argument.getValue().toIdentifier());
    }

    @Test
    void handle_zeroRating_courseReviewSaved() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 0.0, "comment");

        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(reviewableCourse, "id", 11);
        ReflectionTestUtils.setField(reviewableCourse, "originalCourseId", courseId);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 22);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }

    @Test
    void handle_nullComment_courseReviewSaved() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, null);

        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(reviewableCourse, "id", 11);
        ReflectionTestUtils.setField(reviewableCourse, "originalCourseId", courseId);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 22);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("comment", new Comment(null));
    }

    @Test
    void handle_maxRating_courseReviewSaved() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 5.0, "excellent");

        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(reviewableCourse, "id", 11);
        ReflectionTestUtils.setField(reviewableCourse, "originalCourseId", courseId);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 22);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0));
    }

    @Test
    void handle_ratingAboveMax_constraintViolationException() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 5.1, "comment");

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_emptyComment_courseReviewSaved() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "");

        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(reviewableCourse, "id", 11);
        ReflectionTestUtils.setField(reviewableCourse, "originalCourseId", courseId);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 22);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("comment", new Comment(""));
    }

    @Test
    void handle_validCommand_saveCalledExactlyOnce() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(reviewableCourse, "id", 11);
        ReflectionTestUtils.setField(reviewableCourse, "originalCourseId", courseId);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 22);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        sut.handle(command);

        // then
        verify(courseReviewRepository, org.mockito.Mockito.times(1)).save(org.mockito.ArgumentMatchers.any(CourseReview.class));
    }

    @Test
    void handle_nullCourseId_constraintViolationException() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(null, 4.0, "comment");

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_validCommand_courseAndReviewerRefsStoredInSavedEntity() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(reviewableCourse, "id", 11);
        ReflectionTestUtils.setField(reviewableCourse, "originalCourseId", courseId);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 22);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

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
    void handle_ratingExactlyMax_constraintViolationNotThrown() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 5.0, "max boundary");

        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(reviewableCourse, "id", 11);
        ReflectionTestUtils.setField(reviewableCourse, "originalCourseId", courseId);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 22);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    void handle_invalidCommand_courseReviewRepositoryNotConsulted() {
        // given — null courseId triggers ConstraintViolationException; save must NOT be called
        final ReviewCourseCommand command = new ReviewCourseCommand(null, 4.0, "comment");

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        org.mockito.Mockito.verifyNoInteractions(courseReviewRepository);
    }

    @Test
    void handle_fractionalRating_courseReviewSaved() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 2.5, "decent");

        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(reviewableCourse, "id", 11);
        ReflectionTestUtils.setField(reviewableCourse, "originalCourseId", courseId);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 22);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("rating", new CourseRating(2.5));
    }

    @Test
    void handle_ratingExactlySix_constraintViolationException() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 6.0, "comment");

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_courseNotFound_saveNotCalled() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.empty());

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class).isThrownBy(handle);
        org.mockito.Mockito.verifyNoInteractions(courseReviewRepository);
    }

    @Test
    void handle_ratingJustBelowZero_constraintViolationException() {
        // given — -0.1 is just below the @PositiveOrZero boundary
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, -0.1, "comment");

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_twoCalls_differentUuidsReturned() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(reviewableCourse, "id", 11);
        ReflectionTestUtils.setField(reviewableCourse, "originalCourseId", courseId);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 22);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final UUID first = sut.handle(command);
        final UUID second = sut.handle(command);

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void handle_repositorySaveThrows_exceptionPropagates() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(reviewableCourse, "id", 11);
        ReflectionTestUtils.setField(reviewableCourse, "originalCourseId", courseId);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 22);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        when(courseReviewRepository.save(org.mockito.ArgumentMatchers.any(CourseReview.class)))
                .thenThrow(new RuntimeException("db error"));

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(handle).withMessage("db error");
    }

    @Test
    void handle_currentUserAsReviewerThrows_exceptionPropagates() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(reviewableCourse, "id", 11);
        ReflectionTestUtils.setField(reviewableCourse, "originalCourseId", courseId);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        when(currentUserAsReviewer.userAsReviewer()).thenThrow(new RuntimeException("security context error"));

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(handle).withMessage("security context error");
        org.mockito.Mockito.verifyNoInteractions(courseReviewRepository);
    }

    @Test
    void handle_courseNotFound_exceptionMessageContainsUuid() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.empty());

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class)
                .isThrownBy(handle)
                .withMessageContaining(courseId.toString());
    }

    @Test
    void handle_validCommand_reviewerLookedUpExactlyOnce() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(reviewableCourse, "id", 11);
        ReflectionTestUtils.setField(reviewableCourse, "originalCourseId", courseId);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 22);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        sut.handle(command);

        // then
        verify(currentUserAsReviewer, org.mockito.Mockito.times(1)).userAsReviewer();
    }

    @Test
    void handle_validCommand_courseLookedUpExactlyOnce() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(reviewableCourse, "id", 11);
        ReflectionTestUtils.setField(reviewableCourse, "originalCourseId", courseId);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 22);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        sut.handle(command);

        // then
        verify(reviewableCourseRepository, org.mockito.Mockito.times(1)).findByOriginalCourseId(courseId);
    }

    @Test
    void handle_courseNotFound_currentUserAsReviewerNotConsulted() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.empty());

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(com.educational.platform.common.exception.RelatedResourceIsNotResolvedException.class).isThrownBy(handle);
        org.mockito.Mockito.verifyNoInteractions(currentUserAsReviewer);
    }

    @Test
    void handle_repositoryFindByOriginalCourseIdThrows_exceptionPropagates() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");
        when(reviewableCourseRepository.findByOriginalCourseId(courseId))
                .thenThrow(new RuntimeException("db connection lost"));

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(handle).withMessage("db connection lost");
        org.mockito.Mockito.verifyNoInteractions(courseReviewRepository);
    }

    @Test
    void handle_validCommand_courseNotFoundExceptionMessageHasExpectedFormat() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.empty());

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class)
                .isThrownBy(handle)
                .withMessage("Course cannot be found by uuid = " + courseId);
    }
}
