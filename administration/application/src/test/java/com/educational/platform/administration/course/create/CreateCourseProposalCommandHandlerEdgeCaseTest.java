package com.educational.platform.administration.course.create;

import com.educational.platform.administration.course.CourseProposal;
import com.educational.platform.administration.course.CourseProposalRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateCourseProposalCommandHandlerEdgeCaseTest {

    @Mock
    private CourseProposalRepository courseProposalRepository;

    @InjectMocks
    private CreateCourseProposalCommandHandler sut;

    @Test
    void handle_validCommand_proposalSavedWithCorrectUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseProposal> argument = ArgumentCaptor.forClass(CourseProposal.class);
        verify(courseProposalRepository).save(argument.capture());
        final CourseProposal savedProposal = argument.getValue();
        assertThat(savedProposal)
                .hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void handle_twoDifferentProposals_bothSaved() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CreateCourseProposalCommand command1 = new CreateCourseProposalCommand(uuid1);
        final CreateCourseProposalCommand command2 = new CreateCourseProposalCommand(uuid2);

        // when
        sut.handle(command1);
        sut.handle(command2);

        // then
        final ArgumentCaptor<CourseProposal> argument = ArgumentCaptor.forClass(CourseProposal.class);
        verify(courseProposalRepository, org.mockito.Mockito.times(2)).save(argument.capture());
        assertThat(argument.getAllValues()).hasSize(2);
        assertThat(argument.getAllValues().get(0)).hasFieldOrPropertyWithValue("uuid", uuid1);
        assertThat(argument.getAllValues().get(1)).hasFieldOrPropertyWithValue("uuid", uuid2);
    }
}
