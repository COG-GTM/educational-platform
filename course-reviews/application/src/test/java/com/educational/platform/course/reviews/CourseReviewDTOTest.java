package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseReviewDTOTest {

    @Test
    void constructor_allFields_fieldsStored() {
        // given
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseReviewDTO dto = new CourseReviewDTO(reviewUuid, courseUuid, "username", "great course", 4.5);

        // then
        assertThat(dto.uuid()).isEqualTo(reviewUuid);
        assertThat(dto.course()).isEqualTo(courseUuid);
        assertThat(dto.username()).isEqualTo("username");
        assertThat(dto.comment()).isEqualTo("great course");
        assertThat(dto.rating()).isEqualTo(4.5);
    }

    @Test
    void constructor_nullComment_fieldStored() {
        // given
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseReviewDTO dto = new CourseReviewDTO(reviewUuid, courseUuid, "username", null, 3.0);

        // then
        assertThat(dto.comment()).isNull();
    }

    @Test
    void equals_sameFields_returnsTrue() {
        // given
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseReviewDTO first = new CourseReviewDTO(reviewUuid, courseUuid, "user", "good", 4.0);
        final CourseReviewDTO second = new CourseReviewDTO(reviewUuid, courseUuid, "user", "good", 4.0);

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_differentFields_returnsFalse() {
        // given
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseReviewDTO first = new CourseReviewDTO(reviewUuid, courseUuid, "user1", "good", 4.0);
        final CourseReviewDTO second = new CourseReviewDTO(reviewUuid, courseUuid, "user2", "great", 5.0);

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void constructor_allNullObjectFields_storedAsNull() {
        // when — all reference-type fields are null
        final CourseReviewDTO dto = new CourseReviewDTO(null, null, null, null, 0.0);

        // then
        assertThat(dto.uuid()).isNull();
        assertThat(dto.course()).isNull();
        assertThat(dto.username()).isNull();
        assertThat(dto.comment()).isNull();
        assertThat(dto.rating()).isEqualTo(0.0);
    }

    @Test
    void equals_differentRating_returnsFalse() {
        // given
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseReviewDTO first = new CourseReviewDTO(reviewUuid, courseUuid, "user", "good", 4.0);
        final CourseReviewDTO second = new CourseReviewDTO(reviewUuid, courseUuid, "user", "good", 4.1);

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void equals_differentUuid_returnsFalse() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseReviewDTO first = new CourseReviewDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), courseUuid, "user", "good", 4.0);
        final CourseReviewDTO second = new CourseReviewDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440099"), courseUuid, "user", "good", 4.0);

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void constructor_emptyStringComment_fieldStored() {
        // given
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseReviewDTO dto = new CourseReviewDTO(reviewUuid, courseUuid, "username", "", 3.0);

        // then
        assertThat(dto.comment()).isEmpty();
    }

    @Test
    void constructor_zeroRating_fieldStored() {
        // given
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseReviewDTO dto = new CourseReviewDTO(reviewUuid, courseUuid, "username", "comment", 0.0);

        // then
        assertThat(dto.rating()).isEqualTo(0.0);
    }
}
