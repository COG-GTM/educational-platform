package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseExceptionsEdgeCasesTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void courseCannotBePublishedException_messageContainsUuid() {
        // when
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(UUID_VALUE);

        // then
        assertThat(exception.getMessage())
                .contains(UUID_VALUE.toString())
                .contains("cannot be published");
    }

    @Test
    void courseCannotBePublishedException_isRuntimeException() {
        assertThat(new CourseCannotBePublishedException(UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void courseCannotBePublishedException_messageIndicatesApprovalRequired() {
        // when
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(UUID_VALUE);

        // then
        assertThat(exception.getMessage()).contains("approved by admin");
    }

    @Test
    void courseAlreadyApprovedException_messageContainsUuid() {
        // when
        final CourseAlreadyApprovedException exception = new CourseAlreadyApprovedException(UUID_VALUE);

        // then
        assertThat(exception.getMessage())
                .contains(UUID_VALUE.toString())
                .contains("already approved");
    }

    @Test
    void courseAlreadyApprovedException_isRuntimeException() {
        assertThat(new CourseAlreadyApprovedException(UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void courseAlreadyApprovedException_messageIndicatesCannotSendForApproval() {
        // when
        final CourseAlreadyApprovedException exception = new CourseAlreadyApprovedException(UUID_VALUE);

        // then
        assertThat(exception.getMessage()).contains("cannot be sent for approval");
    }

    @Test
    void courseCannotBePublishedException_differentUuids_produceDifferentMessages() {
        final UUID uuid1 = UUID.randomUUID();
        final UUID uuid2 = UUID.randomUUID();

        assertThat(new CourseCannotBePublishedException(uuid1).getMessage())
                .isNotEqualTo(new CourseCannotBePublishedException(uuid2).getMessage());
    }

    @Test
    void courseAlreadyApprovedException_differentUuids_produceDifferentMessages() {
        final UUID uuid1 = UUID.randomUUID();
        final UUID uuid2 = UUID.randomUUID();

        assertThat(new CourseAlreadyApprovedException(uuid1).getMessage())
                .isNotEqualTo(new CourseAlreadyApprovedException(uuid2).getMessage());
    }
}
