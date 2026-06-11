package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    void differentUuids_produceDifferentMessages() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseProposalAlreadyApprovedException exception1 = new CourseProposalAlreadyApprovedException(uuid1);
        final CourseProposalAlreadyApprovedException exception2 = new CourseProposalAlreadyApprovedException(uuid2);

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
        final CourseProposalAlreadyApprovedException exception = new CourseProposalAlreadyApprovedException(uuid);

        // then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).isNotInstanceOf(java.io.IOException.class);
    }

    @Test
    void uuidField_preservesValue() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalAlreadyApprovedException exception = new CourseProposalAlreadyApprovedException(uuid);

        // then
        final UUID storedUuid = (UUID) ReflectionTestUtils.getField(exception, "uuid");
        assertThat(storedUuid).isEqualTo(uuid);
    }

    @Test
    void uuidField_nullUuid_preservesNull() {
        // when
        final CourseProposalAlreadyApprovedException exception = new CourseProposalAlreadyApprovedException(null);

        // then
        final UUID storedUuid = (UUID) ReflectionTestUtils.getField(exception, "uuid");
        assertThat(storedUuid).isNull();
    }
}
