package com.educational.platform.administration.course.create;

import com.educational.platform.administration.course.CourseProposal;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.CourseProposalStatus;
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
public class CreateCourseProposalCommandHandlerUnitTest {

    @Mock
    private CourseProposalRepository courseProposalRepository;

    private CreateCourseProposalCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new CreateCourseProposalCommandHandler(courseProposalRepository);
    }

    @Test
    void handle_validCommand_savesProposalWithWaitingForApprovalStatus() {
        // given
        final UUID uuid = UUID.randomUUID();
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseProposal> captor = ArgumentCaptor.forClass(CourseProposal.class);
        verify(courseProposalRepository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.WAITING_FOR_APPROVAL);
    }
}
