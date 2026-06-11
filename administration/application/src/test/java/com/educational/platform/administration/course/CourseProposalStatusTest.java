package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class CourseProposalStatusTest {

    @Test
    void toDTO_waitingForApproval_correspondingDTO() {
        assertThat(CourseProposalStatus.WAITING_FOR_APPROVAL.toDTO())
                .isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void toDTO_approved_correspondingDTO() {
        assertThat(CourseProposalStatus.APPROVED.toDTO())
                .isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void toDTO_declined_correspondingDTO() {
        assertThat(CourseProposalStatus.DECLINED.toDTO())
                .isEqualTo(CourseProposalStatusDTO.DECLINED);
    }

    @ParameterizedTest
    @EnumSource(CourseProposalStatus.class)
    void toDTO_allValues_nonNull(CourseProposalStatus status) {
        assertThat(status.toDTO()).isNotNull();
    }
}
