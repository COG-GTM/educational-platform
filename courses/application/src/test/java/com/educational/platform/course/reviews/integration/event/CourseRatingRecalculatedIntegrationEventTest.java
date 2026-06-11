package com.educational.platform.course.reviews.integration.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseRatingRecalculatedIntegrationEventTest {

    @Test
    void constructor_validArguments_eventCreated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final double rating = 4.5;

        // when
        final CourseRatingRecalculatedIntegrationEvent sut = new CourseRatingRecalculatedIntegrationEvent(courseId, rating);

        // then
        assertThat(sut.courseId()).isEqualTo(courseId);
        assertThat(sut.rating()).isEqualTo(4.5);
    }

    @Test
    void constructor_zeroRating_eventCreated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseRatingRecalculatedIntegrationEvent sut = new CourseRatingRecalculatedIntegrationEvent(courseId, 0.0);

        // then
        assertThat(sut.rating()).isEqualTo(0.0);
    }

    @Test
    void equality_sameValues_equal() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseRatingRecalculatedIntegrationEvent event1 = new CourseRatingRecalculatedIntegrationEvent(courseId, 4.5);
        final CourseRatingRecalculatedIntegrationEvent event2 = new CourseRatingRecalculatedIntegrationEvent(courseId, 4.5);

        // then
        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void equality_differentRating_notEqual() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseRatingRecalculatedIntegrationEvent event1 = new CourseRatingRecalculatedIntegrationEvent(courseId, 4.5);
        final CourseRatingRecalculatedIntegrationEvent event2 = new CourseRatingRecalculatedIntegrationEvent(courseId, 3.0);

        // then
        assertThat(event1).isNotEqualTo(event2);
    }
}
