package com.educational.platform.administration.course.create;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CreateCourseProposalCommandTest {

    @Test
    void uuid_preservesValue() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);

        // then
        assertThat(command.uuid()).isEqualTo(uuid);
    }

    @Test
    void uuid_nullUuid_preservesNull() {
        // when
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(null);

        // then
        assertThat(command.uuid()).isNull();
    }

    @Test
    void recordEquality_sameUuid_equal() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CreateCourseProposalCommand command1 = new CreateCourseProposalCommand(uuid);
        final CreateCourseProposalCommand command2 = new CreateCourseProposalCommand(uuid);

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
        final CreateCourseProposalCommand command1 = new CreateCourseProposalCommand(uuid1);
        final CreateCourseProposalCommand command2 = new CreateCourseProposalCommand(uuid2);

        // then
        assertThat(command1).isNotEqualTo(command2);
    }

    @Test
    void toString_containsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);

        // then
        assertThat(command.toString()).contains(uuid.toString());
    }
}
