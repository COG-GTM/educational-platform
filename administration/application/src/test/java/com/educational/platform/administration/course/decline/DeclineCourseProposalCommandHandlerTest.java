package com.educational.platform.administration.course.decline;

import com.educational.platform.administration.course.CourseProposal;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.CourseProposalStatus;
import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import com.educational.platform.administration.integration.event.CourseDeclinedByAdminIntegrationEvent;
import com.educational.platform.common.exception.ResourceNotFoundException;
import org.assertj.core.api.ThrowableAssert;
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeclineCourseProposalCommandHandlerTest {

    @Mock
    private CourseProposalRepository repository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DeclineCourseProposalCommandHandler sut;

    @BeforeEach
    void setUp() {
        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        sut = new DeclineCourseProposalCommandHandler(transactionTemplate, repository, eventPublisher);
    }

    @Test
    void handle_existingProposal_declinesAndPublishesEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
        ReflectionTestUtils.setField(proposal, "uuid", uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(proposal));

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseProposal> proposalCaptor = ArgumentCaptor.forClass(CourseProposal.class);
        verify(repository).save(proposalCaptor.capture());
        assertThat(proposalCaptor.getValue())
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.DECLINED);

        final ArgumentCaptor<CourseDeclinedByAdminIntegrationEvent> captor =
                ArgumentCaptor.forClass(CourseDeclinedByAdminIntegrationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().courseId()).isEqualTo(uuid);
    }

    @Test
    void handle_proposalNotFound_resourceNotFoundException() {
        // given
        final UUID uuid = UUID.randomUUID();
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
    }
}
