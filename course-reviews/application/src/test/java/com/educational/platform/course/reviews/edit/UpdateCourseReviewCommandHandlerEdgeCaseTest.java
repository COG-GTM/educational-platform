package com.educational.platform.course.reviews.edit;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewRepository;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class UpdateCourseReviewCommandHandlerEdgeCaseTest {

    @Mock
    private CourseReviewRepository courseReviewRepository;

    private UpdateCourseReviewCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new UpdateCourseReviewCommandHandler(validator, courseReviewRepository);
    }

    @Test
    void handle_reviewNotFound_resourceNotFoundException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 4.0, "Great");
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
    }

    @Test
    void handle_nullRating_constraintViolationException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, null, "comment");
        final CourseReview review = org.mockito.Mockito.mock(CourseReview.class);
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.of(review));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_ratingExceedsMax_constraintViolationException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 6.0, "comment");
        final CourseReview review = org.mockito.Mockito.mock(CourseReview.class);
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.of(review));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_negativeRating_constraintViolationException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, -1.0, "comment");
        final CourseReview review = org.mockito.Mockito.mock(CourseReview.class);
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.of(review));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_validCommand_reviewUpdatedAndSaved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 4.5, "Updated comment");
        final CourseReview review = org.mockito.Mockito.mock(CourseReview.class);
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.of(review));

        // when
        sut.handle(command);

        // then
        verify(review).update(command);
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        assertThat(argument.getValue()).isSameAs(review);
    }
}
