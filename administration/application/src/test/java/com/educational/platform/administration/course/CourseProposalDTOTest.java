package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseProposalDTOTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void constructor_withStatusDTO_keepsProvidedValues() {
        // when
        final CourseProposalDTO dto = new CourseProposalDTO(UUID_VALUE, CourseProposalStatusDTO.APPROVED);

        // then
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void constructor_withDomainStatus_convertsStatusToDTO() {
        // when
        final CourseProposalDTO dto = new CourseProposalDTO(UUID_VALUE, CourseProposalStatus.DECLINED);

        // then
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.DECLINED);
    }

    @Test
    void equals_sameValues_areEqual() {
        // given
        final CourseProposalDTO first = new CourseProposalDTO(UUID_VALUE, CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
        final CourseProposalDTO second = new CourseProposalDTO(UUID_VALUE, CourseProposalStatus.WAITING_FOR_APPROVAL);

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }
}
