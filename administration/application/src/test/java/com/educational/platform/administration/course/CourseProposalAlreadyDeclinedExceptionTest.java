package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseProposalAlreadyDeclinedExceptionTest {

    @Test
    void getMessage_containsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalAlreadyDeclinedException exception = new CourseProposalAlreadyDeclinedException(uuid);

        // then
        assertThat(exception.getMessage())
                .contains(uuid.toString())
                .contains("cannot be declined")
                .contains("already declined");
    }

    @Test
    void isRuntimeException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalAlreadyDeclinedException exception = new CourseProposalAlreadyDeclinedException(uuid);

        // then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getMessage_nullUuid_doesNotThrow() {
        // when
        final CourseProposalAlreadyDeclinedException exception = new CourseProposalAlreadyDeclinedException(null);

        // then
        assertThat(exception.getMessage())
                .contains("null")
                .contains("cannot be declined")
                .contains("already declined");
    }

    @Test
    void getCause_isNull() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalAlreadyDeclinedException exception = new CourseProposalAlreadyDeclinedException(uuid);

        // then
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void getMessage_exactFormat() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalAlreadyDeclinedException exception = new CourseProposalAlreadyDeclinedException(uuid);

        // then
        assertThat(exception.getMessage())
                .isEqualTo("Course Proposal with uuid = " + uuid + " cannot be declined, course proposal was already declined");
    }

    @Test
    void differentUuids_produceDifferentMessages() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseProposalAlreadyDeclinedException exception1 = new CourseProposalAlreadyDeclinedException(uuid1);
        final CourseProposalAlreadyDeclinedException exception2 = new CourseProposalAlreadyDeclinedException(uuid2);

        // then
        assertThat(exception1.getMessage()).isNotEqualTo(exception2.getMessage());
        assertThat(exception1.getMessage()).contains(uuid1.toString());
        assertThat(exception2.getMessage()).contains(uuid2.toString());
    }

    @Test
    void isNotCheckedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalAlreadyDeclinedException exception = new CourseProposalAlreadyDeclinedException(uuid);

        // then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).isNotInstanceOf(java.io.IOException.class);
    }
}
