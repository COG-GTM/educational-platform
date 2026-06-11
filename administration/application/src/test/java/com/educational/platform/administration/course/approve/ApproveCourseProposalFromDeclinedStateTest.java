package com.educational.platform.administration.course.approve;

import com.educational.platform.administration.course.CourseProposal;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.CourseProposalStatus;
import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that approving a previously-declined proposal succeeds at the handler level
 * and publishes the correct integration event.
 */
@ExtendWith(MockitoExtension.class)
public class ApproveCourseProposalFromDeclinedStateTest {

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
    void handle_previouslyDeclinedProposal_approvesAndPublishesEvent() {
        // given
        final UUID uuid = UUID.randomUUID();
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
        proposal.decline();
        ReflectionTestUtils.setField(proposal, "uuid", uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(proposal));

        // when
        sut.handle(new ApproveCourseProposalCommand(uuid));

        // then
        final ArgumentCaptor<CourseProposal> proposalCaptor = ArgumentCaptor.forClass(CourseProposal.class);
        verify(repository).save(proposalCaptor.capture());
        assertThat(proposalCaptor.getValue())
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.APPROVED);

        final ArgumentCaptor<CourseApprovedByAdminIntegrationEvent> eventCaptor =
                ArgumentCaptor.forClass(CourseApprovedByAdminIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().courseId()).isEqualTo(uuid);
    }
}
