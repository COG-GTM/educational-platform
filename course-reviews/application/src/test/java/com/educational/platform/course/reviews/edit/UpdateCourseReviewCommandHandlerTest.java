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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    void handle_concurrentModificationOnSave_optimisticLockingFailurePropagated() {
        // given - an existing review whose save loses an optimistic-lock race: another writer already
        // bumped the @Version added in this PR, so persisting the stale state raises the optimistic-lock
        // failure Hibernate maps an OptimisticLockException to
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 3.0, "updated comment");
        when(courseReviewRepository.save(any(CourseReview.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(CourseReview.class, uuid));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then - the update use case surfaces the conflict to the caller instead of swallowing it or
        // reporting a successful update, so a write that lost the race fails fast as the PR intends
        assertThatExceptionOfType(OptimisticLockingFailureException.class).isThrownBy(handle);
        verify(courseReviewRepository).save(any(CourseReview.class));
    }

    @Test
    void handle_concurrentModificationOnSave_versionSpecificFailureSubtypePropagatedUnchanged() {
        // given - the same lost optimistic-lock race as handle_concurrentModificationOnSave_optimisticLockingFailurePropagated.
        // That test only asserts the generic OptimisticLockingFailureException supertype, so a handler that caught the
        // conflict and rethrew a plain data-access failure would still pass it. This pins that the version-specific
        // ObjectOptimisticLockingFailureException reaches the caller unchanged, still naming the conflicting entity and
        // row, so a version conflict can be told apart from other failures (e.g. mapped to HTTP 409).
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 3.0, "updated comment");
        when(courseReviewRepository.save(any(CourseReview.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(CourseReview.class, uuid));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then - the exact version-specific subtype propagates, still identifying the conflicting CourseReview row
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(handle)
                .satisfies(ex -> {
                    assertThat(ex.getPersistentClassName()).isEqualTo(CourseReview.class.getName());
                    assertThat(ex.getIdentifier()).isEqualTo(uuid);
                });
        verify(courseReviewRepository).save(any(CourseReview.class));
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

    @Test
    void handle_invalidId_noVersionBumpingSavePerformed() {
        // given - no review exists for the command's uuid
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 3.0, "updated comment");
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when - the missing review is updated
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then - the use case fails fast and never reaches save. save() is the only call that flushes a dirty
        // entity, so it is the only call that can advance the @Version added by this PR; verifying it never runs
        // pins that a not-found update can never spuriously persist a row or bump a version.
        // handle_invalidId_resourceNotFoundException only asserts the exception, leaving a handler that still
        // called save before failing undetected.
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
        verify(courseReviewRepository, never()).save(any(CourseReview.class));
    }

    @Test
    void handle_invalidRating_noVersionBumpingSavePerformed() {
        // given - an existing review and a command whose rating violates @NotNull
        final UUID uuid = configureCourseReview();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, null, "updated comment");

        // when - the invalid update is handled
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then - validation rejects the command before it reaches save, so an invalid update never reaches the
        // version-bumping persist: the rejected write performs no save at all.
        // handle_ratingEmpty_resourceNotFoundException only asserts the exception type, not that the save is skipped.
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verify(courseReviewRepository, never()).save(any(CourseReview.class));
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
