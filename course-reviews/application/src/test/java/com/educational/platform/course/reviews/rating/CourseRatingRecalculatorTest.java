package com.educational.platform.course.reviews.rating;

import com.educational.platform.course.reviews.CourseReviewRepository;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseRatingRecalculatorTest {

    private final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CourseRatingRecalculator sut;

    @Test
    void recalculate_ratingsExist_publishesAverage() {
        // given
        when(courseReviewRepository.listRatings(courseUuid)).thenReturn(List.of(4.0, 3.0, 3.5));

        // when
        sut.recalculate(courseUuid);

        // then
        final CourseRatingRecalculatedIntegrationEvent event = publishedEvent();
        assertThat(event.courseId()).isEqualTo(courseUuid);
        assertThat(event.rating()).isCloseTo(3.5, within(0.0001));
    }

    @Test
    void recalculate_noRatings_publishesZero() {
        // given
        when(courseReviewRepository.listRatings(courseUuid)).thenReturn(List.of());

        // when
        sut.recalculate(courseUuid);

        // then
        assertThat(publishedEvent().rating()).isZero();
    }

    @Test
    void recalculateForReview_existingReview_publishesForItsCourse() {
        // given
        when(courseReviewRepository.findCourseUuid(reviewUuid)).thenReturn(Optional.of(courseUuid));
        when(courseReviewRepository.listRatings(courseUuid)).thenReturn(List.of(5.0));

        // when
        sut.recalculateForReview(reviewUuid);

        // then
        assertThat(publishedEvent().courseId()).isEqualTo(courseUuid);
    }

    @Test
    void recalculateForReview_unknownReview_nothingPublished() {
        // given
        when(courseReviewRepository.findCourseUuid(reviewUuid)).thenReturn(Optional.empty());

        // when
        sut.recalculateForReview(reviewUuid);

        // then
        verifyNoInteractions(eventPublisher);
    }

    private CourseRatingRecalculatedIntegrationEvent publishedEvent() {
        final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CourseRatingRecalculatedIntegrationEvent.class);
        return (CourseRatingRecalculatedIntegrationEvent) captor.getValue();
    }
}
