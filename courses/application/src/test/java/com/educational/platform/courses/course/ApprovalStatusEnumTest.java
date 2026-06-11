package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the {@link ApprovalStatus} and {@link PublishStatus} enum values,
 * including valueOf for valid/invalid input and completeness.
 */
public class ApprovalStatusEnumTest {

    @Test
    void approvalStatus_containsAllExpectedValues() {
        assertThat(ApprovalStatus.values()).containsExactly(
                ApprovalStatus.NOT_SENT_FOR_APPROVAL,
                ApprovalStatus.WAITING_FOR_APPROVAL,
                ApprovalStatus.DECLINED,
                ApprovalStatus.APPROVED
        );
    }

    @ParameterizedTest
    @EnumSource(ApprovalStatus.class)
    void approvalStatus_valueOf_roundTrips(ApprovalStatus status) {
        assertThat(ApprovalStatus.valueOf(status.name())).isEqualTo(status);
    }

    @Test
    void approvalStatus_invalidValue_throwsIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ApprovalStatus.valueOf("INVALID"));
    }

    @Test
    void publishStatus_containsAllExpectedValues() {
        assertThat(PublishStatus.values()).containsExactly(
                PublishStatus.DRAFT,
                PublishStatus.PUBLISHED,
                PublishStatus.ARCHIVED
        );
    }

    @ParameterizedTest
    @EnumSource(PublishStatus.class)
    void publishStatus_valueOf_roundTrips(PublishStatus status) {
        assertThat(PublishStatus.valueOf(status.name())).isEqualTo(status);
    }

    @Test
    void publishStatus_invalidValue_throwsIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> PublishStatus.valueOf("INVALID"));
    }
}
