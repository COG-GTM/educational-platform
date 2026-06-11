package com.educational.platform.administration.course.decline;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeclineCourseProposalCommandTest {

    @Test
    void uuid_preservesValue() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        // then
        assertThat(command.uuid()).isEqualTo(uuid);
    }

    @Test
    void uuid_nullUuid_preservesNull() {
        // when
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(null);

        // then
        assertThat(command.uuid()).isNull();
    }

    @Test
    void recordEquality_sameUuid_equal() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final DeclineCourseProposalCommand command1 = new DeclineCourseProposalCommand(uuid);
        final DeclineCourseProposalCommand command2 = new DeclineCourseProposalCommand(uuid);

        // then
        assertThat(command1).isEqualTo(command2);
        assertThat(command1.hashCode()).isEqualTo(command2.hashCode());
    }

    @Test
    void recordEquality_differentUuid_notEqual() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final DeclineCourseProposalCommand command1 = new DeclineCourseProposalCommand(uuid1);
        final DeclineCourseProposalCommand command2 = new DeclineCourseProposalCommand(uuid2);

        // then
        assertThat(command1).isNotEqualTo(command2);
    }
}
