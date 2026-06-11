package com.educational.platform.administration.course.create;

import com.educational.platform.administration.course.CourseProposal;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.CourseProposalStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CreateCourseProposalCommandHandlerTest {

    @Mock
    private CourseProposalRepository repository;

    @InjectMocks
    private CreateCourseProposalCommandHandler sut;

    @Test
    void handle_courseProposalSaved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseProposal> argument = ArgumentCaptor.forClass(CourseProposal.class);
        verify(repository).save(argument.capture());
        final CourseProposal proposal = argument.getValue();
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void handle_courseProposalSavedWithWaitingForApprovalStatus() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseProposal> argument = ArgumentCaptor.forClass(CourseProposal.class);
        verify(repository).save(argument.capture());
        final CourseProposal proposal = argument.getValue();
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void handle_repositorySaveThrows_exceptionPropagates() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);
        doThrow(new RuntimeException("DB error"))
                .when(repository).save(any(CourseProposal.class));

        // when / then
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");
    }

    @Test
    void handle_nullUuidCommand_courseProposalSavedWithNullUuid() {
        // given
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(null);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseProposal> argument = ArgumentCaptor.forClass(CourseProposal.class);
        verify(repository).save(argument.capture());
        final CourseProposal proposal = argument.getValue();
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("uuid", null)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void class_hasTransactionalAnnotation() {
        // then
        assertThat(CreateCourseProposalCommandHandler.class.isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    void handle_doesNotHavePreAuthorizeAnnotation() throws NoSuchMethodException {
        // when
        final var method = CreateCourseProposalCommandHandler.class
                .getMethod("handle", CreateCourseProposalCommand.class);

        // then
        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isFalse();
    }

    @Test
    void class_hasNamedAnnotation() {
        assertThat(CreateCourseProposalCommandHandler.class.isAnnotationPresent(jakarta.inject.Named.class)).isTrue();
    }

    @Test
    void handle_repositorySaveCalledExactlyOnce() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);

        // when
        sut.handle(command);

        // then
        verify(repository, org.mockito.Mockito.times(1)).save(any(CourseProposal.class));
    }

    @Test
    void handle_savedProposalHasCorrectUuidAndWaitingForApprovalStatus() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseProposal> argument = ArgumentCaptor.forClass(CourseProposal.class);
        verify(repository).save(argument.capture());
        final CourseProposal proposal = argument.getValue();
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void handle_noMoreRepositoryInteractions() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);

        // when
        sut.handle(command);

        // then
        verify(repository).save(any(CourseProposal.class));
        org.mockito.Mockito.verifyNoMoreInteractions(repository);
    }

    @Test
    void constructor_acceptsOnlyRepository() throws NoSuchMethodException {
        // when
        final var constructor = CreateCourseProposalCommandHandler.class.getConstructor(
                CourseProposalRepository.class
        );

        // then
        assertThat(constructor).isNotNull();
        assertThat(constructor.getParameterCount()).isEqualTo(1);
    }

    @Test
    void class_doesNotInjectEventPublisher() {
        // then - handler has no ApplicationEventPublisher dependency (unlike approve/decline)
        assertThat(CreateCourseProposalCommandHandler.class.getDeclaredConstructors())
                .allSatisfy(ctor -> {
                    for (Class<?> paramType : ctor.getParameterTypes()) {
                        assertThat(paramType).isNotEqualTo(org.springframework.context.ApplicationEventPublisher.class);
                    }
                });
    }

    @Test
    void handle_multipleSequentialCommands_eachSavedSeparately() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CreateCourseProposalCommand command1 = new CreateCourseProposalCommand(uuid1);
        final CreateCourseProposalCommand command2 = new CreateCourseProposalCommand(uuid2);

        // when
        sut.handle(command1);
        sut.handle(command2);

        // then
        final ArgumentCaptor<CourseProposal> captor = ArgumentCaptor.forClass(CourseProposal.class);
        verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0))
                .hasFieldOrPropertyWithValue("uuid", uuid1)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.WAITING_FOR_APPROVAL);
        assertThat(captor.getAllValues().get(1))
                .hasFieldOrPropertyWithValue("uuid", uuid2)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.WAITING_FOR_APPROVAL);
    }
}
