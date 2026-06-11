package com.educational.platform.course.reviews.integration.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseRatingRecalculatedIntegrationEventTest {

    @Test
    void constructor_validArguments_fieldsStored() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(courseId, 4.5);

        // then
        assertThat(event.courseId()).isEqualTo(courseId);
        assertThat(event.rating()).isEqualTo(4.5);
    }

    @Test
    void constructor_nullCourseId_nullStored() {
        // when
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(null, 3.0);

        // then
        assertThat(event.courseId()).isNull();
    }

    @Test
    void constructor_zeroRating_zeroStored() {
        // when
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(courseId, 0.0);

        // then
        assertThat(event.rating()).isEqualTo(0.0);
    }

    @Test
    void constructor_maxRating_ratingStored() {
        // when
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(courseId, 5.0);

        // then
        assertThat(event.rating()).isEqualTo(5.0);
    }

    @Test
    void equals_sameFields_returnsTrue() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent first = new CourseRatingRecalculatedIntegrationEvent(courseId, 4.0);
        final CourseRatingRecalculatedIntegrationEvent second = new CourseRatingRecalculatedIntegrationEvent(courseId, 4.0);

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_differentCourseId_returnsFalse() {
        // given
        final CourseRatingRecalculatedIntegrationEvent first = new CourseRatingRecalculatedIntegrationEvent(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), 4.0);
        final CourseRatingRecalculatedIntegrationEvent second = new CourseRatingRecalculatedIntegrationEvent(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440002"), 4.0);

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void equals_differentRating_returnsFalse() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent first = new CourseRatingRecalculatedIntegrationEvent(courseId, 3.0);
        final CourseRatingRecalculatedIntegrationEvent second = new CourseRatingRecalculatedIntegrationEvent(courseId, 4.0);

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void constructor_negativeRating_ratingStored() {
        // when
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(courseId, -1.0);

        // then
        assertThat(event.rating()).isEqualTo(-1.0);
    }

    @Test
    void constructor_fractionalRating_ratingStored() {
        // when
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(courseId, 3.75);

        // then
        assertThat(event.rating()).isEqualTo(3.75);
    }
}
