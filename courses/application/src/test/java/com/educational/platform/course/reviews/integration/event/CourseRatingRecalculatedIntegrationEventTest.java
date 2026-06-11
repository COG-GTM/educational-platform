package com.educational.platform.course.reviews.integration.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link CourseRatingRecalculatedIntegrationEvent} record accessors and equality.
 */
public class CourseRatingRecalculatedIntegrationEventTest {

    @Test
    void accessors_returnProvidedValues() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseRatingRecalculatedIntegrationEvent event =
                new CourseRatingRecalculatedIntegrationEvent(courseId, 4.5);

        // then
        assertThat(event.courseId()).isEqualTo(courseId);
        assertThat(event.rating()).isEqualTo(4.5);
    }

    @Test
    void equals_sameValues_areEqual() {
        final UUID courseId = UUID.randomUUID();
        assertThat(new CourseRatingRecalculatedIntegrationEvent(courseId, 3.0))
                .isEqualTo(new CourseRatingRecalculatedIntegrationEvent(courseId, 3.0));
    }

    @Test
    void equals_differentCourseId_areNotEqual() {
        assertThat(new CourseRatingRecalculatedIntegrationEvent(UUID.randomUUID(), 3.0))
                .isNotEqualTo(new CourseRatingRecalculatedIntegrationEvent(UUID.randomUUID(), 3.0));
    }

    @Test
    void equals_differentRating_areNotEqual() {
        final UUID courseId = UUID.randomUUID();
        assertThat(new CourseRatingRecalculatedIntegrationEvent(courseId, 3.0))
                .isNotEqualTo(new CourseRatingRecalculatedIntegrationEvent(courseId, 4.0));
    }

    @Test
    void rating_zeroValue_preserved() {
        final UUID courseId = UUID.randomUUID();
        final CourseRatingRecalculatedIntegrationEvent event =
                new CourseRatingRecalculatedIntegrationEvent(courseId, 0.0);
        assertThat(event.rating()).isEqualTo(0.0);
    }

    @Test
    void rating_maxBoundary_preserved() {
        final UUID courseId = UUID.randomUUID();
        final CourseRatingRecalculatedIntegrationEvent event =
                new CourseRatingRecalculatedIntegrationEvent(courseId, 5.0);
        assertThat(event.rating()).isEqualTo(5.0);
    }
}
