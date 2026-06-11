package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApprovalStatusTest {

    @Test
    void values_containsAllExpectedStatuses() {
        // then
        assertThat(ApprovalStatus.values()).containsExactlyInAnyOrder(
                ApprovalStatus.NOT_SENT_FOR_APPROVAL,
                ApprovalStatus.WAITING_FOR_APPROVAL,
                ApprovalStatus.DECLINED,
                ApprovalStatus.APPROVED
        );
    }

    @Test
    void values_hasFourStatuses() {
        // then
        assertThat(ApprovalStatus.values()).hasSize(4);
    }

    @Test
    void valueOf_validName_returnsCorrectEnum() {
        // then
        assertThat(ApprovalStatus.valueOf("NOT_SENT_FOR_APPROVAL"))
                .isEqualTo(ApprovalStatus.NOT_SENT_FOR_APPROVAL);
        assertThat(ApprovalStatus.valueOf("WAITING_FOR_APPROVAL"))
                .isEqualTo(ApprovalStatus.WAITING_FOR_APPROVAL);
        assertThat(ApprovalStatus.valueOf("DECLINED"))
                .isEqualTo(ApprovalStatus.DECLINED);
        assertThat(ApprovalStatus.valueOf("APPROVED"))
                .isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void valueOf_invalidName_throwsIllegalArgumentException() {
        // then
        assertThatThrownBy(() -> ApprovalStatus.valueOf("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void name_returnsCorrectStringRepresentation() {
        // then
        assertThat(ApprovalStatus.NOT_SENT_FOR_APPROVAL.name()).isEqualTo("NOT_SENT_FOR_APPROVAL");
        assertThat(ApprovalStatus.APPROVED.name()).isEqualTo("APPROVED");
    }

    @Test
    void ordinal_valuesAreSequential() {
        // then
        assertThat(ApprovalStatus.NOT_SENT_FOR_APPROVAL.ordinal()).isEqualTo(0);
        assertThat(ApprovalStatus.WAITING_FOR_APPROVAL.ordinal()).isEqualTo(1);
        assertThat(ApprovalStatus.DECLINED.ordinal()).isEqualTo(2);
        assertThat(ApprovalStatus.APPROVED.ordinal()).isEqualTo(3);
    }
}
