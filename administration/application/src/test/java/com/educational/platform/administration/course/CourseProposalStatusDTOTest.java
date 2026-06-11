package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the {@link CourseProposalStatusDTO} enum values and valueOf.
 */
public class CourseProposalStatusDTOTest {

    @Test
    void containsExpectedValues() {
        assertThat(CourseProposalStatusDTO.values())
                .containsExactly(
                        CourseProposalStatusDTO.WAITING_FOR_APPROVAL,
                        CourseProposalStatusDTO.DECLINED,
                        CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void valueOf_waitingForApproval() {
        assertThat(CourseProposalStatusDTO.valueOf("WAITING_FOR_APPROVAL"))
                .isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void valueOf_declined() {
        assertThat(CourseProposalStatusDTO.valueOf("DECLINED"))
                .isEqualTo(CourseProposalStatusDTO.DECLINED);
    }

    @Test
    void valueOf_approved() {
        assertThat(CourseProposalStatusDTO.valueOf("APPROVED"))
                .isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void valueOf_invalid_throwsException() {
        assertThatThrownBy(() -> CourseProposalStatusDTO.valueOf("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void valuesCount() {
        assertThat(CourseProposalStatusDTO.values()).hasSize(3);
    }
}
