package com.educational.platform.courses.course.approve;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class SendCourseToApproveCommandTest {

    @Test
    void constructor_uuidStored() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final SendCourseToApproveCommand sut = new SendCourseToApproveCommand(uuid);

        // then
        assertThat(sut.uuid()).isEqualTo(uuid);
    }

    @Test
    void constructor_nullUuid_storedAsNull() {
        // when
        final SendCourseToApproveCommand sut = new SendCourseToApproveCommand(null);

        // then
        assertThat(sut.uuid()).isNull();
    }

    @Test
    void equalInstances_areEqual() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveCommand a = new SendCourseToApproveCommand(uuid);
        final SendCourseToApproveCommand b = new SendCourseToApproveCommand(uuid);

        // then
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void differentInstances_areNotEqual() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");

        // then
        assertThat(new SendCourseToApproveCommand(uuid1)).isNotEqualTo(new SendCourseToApproveCommand(uuid2));
    }
}
