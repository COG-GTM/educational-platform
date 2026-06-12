package com.educational.platform.course.reviews;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.create.ReviewCourseCommandHandler;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommandHandler;
import com.educational.platform.course.reviews.query.ListCourseReviewsByCourseUUIDQueryHandler;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseReviewControllerEdgeCaseTest {

    @Mock
    private ReviewCourseCommandHandler reviewCourseCommandHandler;

    @Mock
    private UpdateCourseReviewCommandHandler updateCourseReviewCommandHandler;

    @Mock
    private ListCourseReviewsByCourseUUIDQueryHandler listCourseReviewsByCourseUUIDQueryHandler;

    @InjectMocks
    private CourseReviewController sut;

    @Test
    void review_handlerThrowsRelatedResourceNotResolved_exceptionPropagated() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "comment");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class)))
                .thenThrow(new RelatedResourceIsNotResolvedException("Course cannot be found by uuid = " + courseUuid));

        // when
        final ThrowableAssert.ThrowingCallable review = () -> sut.review(courseUuid, request);

        // then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class).isThrownBy(review);
    }

    @Test
    void review_handlerThrowsConstraintViolation_exceptionPropagated() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "comment");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class)))
                .thenThrow(new ConstraintViolationException(Set.of()));

        // when
        final ThrowableAssert.ThrowingCallable review = () -> sut.review(courseUuid, request);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(review);
    }

    @Test
    void updateReview_handlerThrowsResourceNotFound_exceptionPropagated() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, "updated");
        doThrow(new ResourceNotFoundException("Course Review with uuid: " + reviewUuid + " not found"))
                .when(updateCourseReviewCommandHandler).handle(any(UpdateCourseReviewCommand.class));

        // when
        final ThrowableAssert.ThrowingCallable update = () -> sut.updateReview(courseUuid, reviewUuid, request);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(update);
    }

    @Test
    void updateReview_handlerThrowsConstraintViolation_exceptionPropagated() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(6.0, "invalid");
        doThrow(new ConstraintViolationException(Set.of()))
                .when(updateCourseReviewCommandHandler).handle(any(UpdateCourseReviewCommand.class));

        // when
        final ThrowableAssert.ThrowingCallable update = () -> sut.updateReview(courseUuid, reviewUuid, request);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(update);
    }

    @Test
    void review_noInteractionWithUpdateOrQueryHandlers() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "comment");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class))).thenReturn(UUID.randomUUID());

        // when
        sut.review(courseUuid, request);

        // then
        verifyNoInteractions(updateCourseReviewCommandHandler);
        verifyNoInteractions(listCourseReviewsByCourseUUIDQueryHandler);
    }

    @Test
    void updateReview_noInteractionWithCreateOrQueryHandlers() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.5, "updated");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        verifyNoInteractions(reviewCourseCommandHandler);
        verifyNoInteractions(listCourseReviewsByCourseUUIDQueryHandler);
    }
}
