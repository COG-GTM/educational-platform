package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseProposalAlreadyApprovedExceptionTest {

    @Test
    void constructor_validUuid_exceptionCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalAlreadyApprovedException sut = new CourseProposalAlreadyApprovedException(uuid);

        // then
        assertThat(sut).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getMessage_validUuid_containsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposalAlreadyApprovedException sut = new CourseProposalAlreadyApprovedException(uuid);

        // when
        final String message = sut.getMessage();

        // then
        assertThat(message).isEqualTo("Course Proposal with uuid = 123e4567-e89b-12d3-a456-426655440001 cannot be approved, course proposal was already approved");
    }
}
