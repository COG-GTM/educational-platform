package com.educational.platform.course.reviews.create;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewFactory;
import com.educational.platform.course.reviews.CourseReviewRepository;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewCourseCommandHandlerEdgeCaseTest {

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private CourseReviewFactory courseReviewFactory;

    @InjectMocks
    private ReviewCourseCommandHandler sut;

    @Test
    void handle_factoryThrowsRelatedResourceNotResolved_exceptionPropagated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "Good course");
        when(courseReviewFactory.createFrom(command))
                .thenThrow(new RelatedResourceIsNotResolvedException("Course cannot be found by uuid = " + courseId));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class).isThrownBy(handle);
    }

    @Test
    void handle_validCommand_reviewSavedAndUuidReturned() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.5, "Excellent");
        final CourseReview review = mock(CourseReview.class);
        final UUID expectedUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440099");
        when(courseReviewFactory.createFrom(command)).thenReturn(review);
        when(review.toIdentifier()).thenReturn(expectedUuid);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isEqualTo(expectedUuid);
        verify(courseReviewRepository).save(review);
    }

    @Test
    void handle_twoReviews_eachSavedSeparately() {
        // given
        final UUID courseId1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseId2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final ReviewCourseCommand command1 = new ReviewCourseCommand(courseId1, 3.0, "OK");
        final ReviewCourseCommand command2 = new ReviewCourseCommand(courseId2, 5.0, "Amazing");
        final CourseReview review1 = mock(CourseReview.class);
        final CourseReview review2 = mock(CourseReview.class);
        when(courseReviewFactory.createFrom(command1)).thenReturn(review1);
        when(courseReviewFactory.createFrom(command2)).thenReturn(review2);
        when(review1.toIdentifier()).thenReturn(UUID.randomUUID());
        when(review2.toIdentifier()).thenReturn(UUID.randomUUID());

        // when
        final UUID result1 = sut.handle(command1);
        final UUID result2 = sut.handle(command2);

        // then
        assertThat(result1).isNotEqualTo(result2);
        verify(courseReviewRepository).save(review1);
        verify(courseReviewRepository).save(review2);
    }
}
