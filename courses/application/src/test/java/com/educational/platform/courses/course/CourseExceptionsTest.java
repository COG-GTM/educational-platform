package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseExceptionsTest {

    @Test
    void courseAlreadyApprovedException_messageContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseAlreadyApprovedException exception = new CourseAlreadyApprovedException(uuid);

        // then
        assertThat(exception.getMessage())
                .contains("123e4567-e89b-12d3-a456-426655440001")
                .contains("cannot be sent for approval")
                .contains("already approved");
    }

    @Test
    void courseCannotBePublishedException_messageContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(uuid);

        // then
        assertThat(exception.getMessage())
                .contains("123e4567-e89b-12d3-a456-426655440002")
                .contains("cannot be published")
                .contains("approved by admin");
    }

    @Test
    void courseAlreadyApprovedException_differentUuids_produceDifferentMessages() {
        // given
        final UUID uuid1 = UUID.randomUUID();
        final UUID uuid2 = UUID.randomUUID();

        // when
        final CourseAlreadyApprovedException ex1 = new CourseAlreadyApprovedException(uuid1);
        final CourseAlreadyApprovedException ex2 = new CourseAlreadyApprovedException(uuid2);

        // then
        assertThat(ex1.getMessage()).contains(uuid1.toString());
        assertThat(ex2.getMessage()).contains(uuid2.toString());
        assertThat(ex1.getMessage()).isNotEqualTo(ex2.getMessage());
    }

    @Test
    void courseCannotBePublishedException_differentUuids_produceDifferentMessages() {
        // given
        final UUID uuid1 = UUID.randomUUID();
        final UUID uuid2 = UUID.randomUUID();

        // when
        final CourseCannotBePublishedException ex1 = new CourseCannotBePublishedException(uuid1);
        final CourseCannotBePublishedException ex2 = new CourseCannotBePublishedException(uuid2);

        // then
        assertThat(ex1.getMessage()).contains(uuid1.toString());
        assertThat(ex2.getMessage()).contains(uuid2.toString());
        assertThat(ex1.getMessage()).isNotEqualTo(ex2.getMessage());
    }
}
