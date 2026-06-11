package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseProposalAlreadyApprovedExceptionTest {

    @Test
    void getMessage_containsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalAlreadyApprovedException exception = new CourseProposalAlreadyApprovedException(uuid);

        // then
        assertThat(exception.getMessage())
                .contains(uuid.toString())
                .contains("cannot be approved")
                .contains("already approved");
    }

    @Test
    void isRuntimeException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalAlreadyApprovedException exception = new CourseProposalAlreadyApprovedException(uuid);

        // then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getMessage_nullUuid_doesNotThrow() {
        // when
        final CourseProposalAlreadyApprovedException exception = new CourseProposalAlreadyApprovedException(null);

        // then
        assertThat(exception.getMessage())
                .contains("null")
                .contains("cannot be approved")
                .contains("already approved");
    }

    @Test
    void getCause_isNull() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalAlreadyApprovedException exception = new CourseProposalAlreadyApprovedException(uuid);

        // then
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void getMessage_exactFormat() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalAlreadyApprovedException exception = new CourseProposalAlreadyApprovedException(uuid);

        // then
        assertThat(exception.getMessage())
                .isEqualTo("Course Proposal with uuid = " + uuid + " cannot be approved, course proposal was already approved");
    }
}
