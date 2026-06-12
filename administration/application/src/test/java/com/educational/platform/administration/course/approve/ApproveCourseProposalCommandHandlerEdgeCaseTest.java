package com.educational.platform.administration.course.approve;

import com.educational.platform.administration.course.CourseProposal;
import com.educational.platform.administration.course.CourseProposalAlreadyApprovedException;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.CourseProposalStatus;
import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import com.educational.platform.common.exception.ResourceNotFoundException;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApproveCourseProposalCommandHandlerEdgeCaseTest {

    @Mock
    private CourseProposalRepository repository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ApproveCourseProposalCommandHandler sut;

    @BeforeEach
    void setUp() {
        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        sut = new ApproveCourseProposalCommandHandler(transactionTemplate, repository, eventPublisher);
    }

    @Test
    void handle_alreadyApprovedProposal_courseProposalAlreadyApprovedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(createCommand);
        ReflectionTestUtils.setField(proposal, "uuid", uuid);
        ReflectionTestUtils.setField(proposal, "status", CourseProposalStatus.APPROVED);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(proposal));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(CourseProposalAlreadyApprovedException.class).isThrownBy(handle);
    }

    @Test
    void handle_proposalNotFound_resourceNotFoundException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
    }
}
