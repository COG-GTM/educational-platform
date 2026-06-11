package com.educational.platform.administration.course.create;

import com.educational.platform.administration.course.CourseProposal;
import com.educational.platform.administration.course.CourseProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CreateCourseProposalCommandHandlerTest {

    @Mock
    private CourseProposalRepository courseProposalRepository;

    private CreateCourseProposalCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new CreateCourseProposalCommandHandler(courseProposalRepository);
    }

    @Test
    void handle_validCommand_savesProposalWithCorrectUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseProposal> captor = ArgumentCaptor.forClass(CourseProposal.class);
        verify(courseProposalRepository).save(captor.capture());
        assertThat(captor.getValue().toDTO().uuid()).isEqualTo(uuid);
    }

    @Test
    void handle_differentUuids_savesDistinctProposals() {
        // given
        final UUID uuid1 = UUID.randomUUID();
        final UUID uuid2 = UUID.randomUUID();

        // when
        sut.handle(new CreateCourseProposalCommand(uuid1));
        sut.handle(new CreateCourseProposalCommand(uuid2));

        // then
        final ArgumentCaptor<CourseProposal> captor = ArgumentCaptor.forClass(CourseProposal.class);
        verify(courseProposalRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues().get(0).toDTO().uuid()).isEqualTo(uuid1);
        assertThat(captor.getAllValues().get(1).toDTO().uuid()).isEqualTo(uuid2);
    }
}
