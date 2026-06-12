package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApprovalStatusTest {

    @Test
    void values_allStatusesExist() {
        // given
        final ApprovalStatus[] values = ApprovalStatus.values();

        // when / then
        assertThat(values).containsExactlyInAnyOrder(
                ApprovalStatus.NOT_SENT_FOR_APPROVAL,
                ApprovalStatus.WAITING_FOR_APPROVAL,
                ApprovalStatus.DECLINED,
                ApprovalStatus.APPROVED
        );
    }

    @Test
    void valueOf_notSentForApproval_correctValue() {
        // when
        final ApprovalStatus status = ApprovalStatus.valueOf("NOT_SENT_FOR_APPROVAL");

        // then
        assertThat(status).isEqualTo(ApprovalStatus.NOT_SENT_FOR_APPROVAL);
    }

    @Test
    void valueOf_waitingForApproval_correctValue() {
        // when
        final ApprovalStatus status = ApprovalStatus.valueOf("WAITING_FOR_APPROVAL");

        // then
        assertThat(status).isEqualTo(ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void valueOf_declined_correctValue() {
        // when
        final ApprovalStatus status = ApprovalStatus.valueOf("DECLINED");

        // then
        assertThat(status).isEqualTo(ApprovalStatus.DECLINED);
    }

    @Test
    void valueOf_approved_correctValue() {
        // when
        final ApprovalStatus status = ApprovalStatus.valueOf("APPROVED");

        // then
        assertThat(status).isEqualTo(ApprovalStatus.APPROVED);
    }
}
