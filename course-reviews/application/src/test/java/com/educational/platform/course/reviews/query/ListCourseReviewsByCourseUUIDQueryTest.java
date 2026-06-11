package com.educational.platform.course.reviews.query;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ListCourseReviewsByCourseUUIDQueryTest {

    @Test
    void constructor_validUuid_uuidStored() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final ListCourseReviewsByCourseUUIDQuery query = new ListCourseReviewsByCourseUUIDQuery(courseId);

        // then
        assertThat(query.uuid()).isEqualTo(courseId);
    }

    @Test
    void constructor_nullUuid_nullStored() {
        // when
        final ListCourseReviewsByCourseUUIDQuery query = new ListCourseReviewsByCourseUUIDQuery(null);

        // then
        assertThat(query.uuid()).isNull();
    }

    @Test
    void equals_sameUuid_returnsTrue() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ListCourseReviewsByCourseUUIDQuery first = new ListCourseReviewsByCourseUUIDQuery(courseId);
        final ListCourseReviewsByCourseUUIDQuery second = new ListCourseReviewsByCourseUUIDQuery(courseId);

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_differentUuid_returnsFalse() {
        // given
        final ListCourseReviewsByCourseUUIDQuery first = new ListCourseReviewsByCourseUUIDQuery(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"));
        final ListCourseReviewsByCourseUUIDQuery second = new ListCourseReviewsByCourseUUIDQuery(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

        // then
        assertThat(first).isNotEqualTo(second);
    }
}
