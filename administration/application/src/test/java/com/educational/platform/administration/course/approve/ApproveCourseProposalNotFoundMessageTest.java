package com.educational.platform.administration.course.approve;

import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link ApproveCourseProposalCommandHandler} includes the
 * UUID in the {@link ResourceNotFoundException} message when the proposal is not found.
 */
@ExtendWith(MockitoExtension.class)
public class ApproveCourseProposalNotFoundMessageTest {

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
    void handle_proposalNotFound_exceptionMessageContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440099");
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> sut.handle(new ApproveCourseProposalCommand(uuid)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(uuid.toString());
    }
}
