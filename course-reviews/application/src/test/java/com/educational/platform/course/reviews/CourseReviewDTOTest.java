package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseReviewDTOTest {

    @Test
    void constructor_validArguments_dtoCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseReviewDTO sut = new CourseReviewDTO(uuid, courseUuid, "reviewer", "Great course!", 4.5);

        // then
        assertThat(sut.uuid()).isEqualTo(uuid);
        assertThat(sut.course()).isEqualTo(courseUuid);
        assertThat(sut.username()).isEqualTo("reviewer");
        assertThat(sut.comment()).isEqualTo("Great course!");
        assertThat(sut.rating()).isEqualTo(4.5);
    }

    @Test
    void constructor_nullComment_dtoCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseReviewDTO sut = new CourseReviewDTO(uuid, courseUuid, "reviewer", null, 3.0);

        // then
        assertThat(sut.comment()).isNull();
        assertThat(sut.rating()).isEqualTo(3.0);
    }

    @Test
    void constructor_zeroRating_dtoCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseReviewDTO sut = new CourseReviewDTO(uuid, courseUuid, "reviewer", "comment", 0.0);

        // then
        assertThat(sut.rating()).isZero();
    }
}
