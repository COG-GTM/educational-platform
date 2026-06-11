package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseReviewDTOTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static final UUID COURSE_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

    @Test
    void constructor_keepsProvidedValues() {
        // when
        final CourseReviewDTO dto = new CourseReviewDTO(UUID_VALUE, COURSE_VALUE, "username", "great course", 4.5);

        // then
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
        assertThat(dto.course()).isEqualTo(COURSE_VALUE);
        assertThat(dto.username()).isEqualTo("username");
        assertThat(dto.comment()).isEqualTo("great course");
        assertThat(dto.rating()).isEqualTo(4.5);
    }

    @Test
    void equals_sameValues_areEqual() {
        // given
        final CourseReviewDTO first = new CourseReviewDTO(UUID_VALUE, COURSE_VALUE, "user", "comment", 3.0);
        final CourseReviewDTO second = new CourseReviewDTO(UUID_VALUE, COURSE_VALUE, "user", "comment", 3.0);

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_differentRating_notEqual() {
        // given
        final CourseReviewDTO first = new CourseReviewDTO(UUID_VALUE, COURSE_VALUE, "user", "comment", 3.0);
        final CourseReviewDTO second = new CourseReviewDTO(UUID_VALUE, COURSE_VALUE, "user", "comment", 4.0);

        // then
        assertThat(first).isNotEqualTo(second);
    }
}
