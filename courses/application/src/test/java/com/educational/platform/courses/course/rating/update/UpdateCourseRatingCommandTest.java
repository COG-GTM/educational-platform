package com.educational.platform.courses.course.rating.update;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateCourseRatingCommandTest {

    @Test
    void constructor_allFieldsStored() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final UpdateCourseRatingCommand sut = new UpdateCourseRatingCommand(uuid, 4.5);

        // then
        assertThat(sut.uuid()).isEqualTo(uuid);
        assertThat(sut.rating()).isEqualTo(4.5);
    }

    @Test
    void constructor_zeroRating_storedCorrectly() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final UpdateCourseRatingCommand sut = new UpdateCourseRatingCommand(uuid, 0.0);

        // then
        assertThat(sut.rating()).isEqualTo(0.0);
    }

    @Test
    void constructor_negativeRating_storedCorrectly() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final UpdateCourseRatingCommand sut = new UpdateCourseRatingCommand(uuid, -1.0);

        // then
        assertThat(sut.rating()).isEqualTo(-1.0);
    }

    @Test
    void constructor_nullUuid_storedAsNull() {
        // when
        final UpdateCourseRatingCommand sut = new UpdateCourseRatingCommand(null, 3.0);

        // then
        assertThat(sut.uuid()).isNull();
    }

    @Test
    void equalInstances_areEqual() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseRatingCommand a = new UpdateCourseRatingCommand(uuid, 4.0);
        final UpdateCourseRatingCommand b = new UpdateCourseRatingCommand(uuid, 4.0);

        // then
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void differentRating_notEqual() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseRatingCommand a = new UpdateCourseRatingCommand(uuid, 4.0);
        final UpdateCourseRatingCommand b = new UpdateCourseRatingCommand(uuid, 5.0);

        // then
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void differentUuid_notEqual() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseRatingCommand a = new UpdateCourseRatingCommand(uuid1, 4.0);
        final UpdateCourseRatingCommand b = new UpdateCourseRatingCommand(uuid2, 4.0);

        // then
        assertThat(a).isNotEqualTo(b);
    }
}
