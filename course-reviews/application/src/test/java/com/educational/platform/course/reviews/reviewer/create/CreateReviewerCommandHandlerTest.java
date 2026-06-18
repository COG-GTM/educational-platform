package com.educational.platform.course.reviews.reviewer.create;

import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.ReviewerRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateReviewerCommandHandlerTest {

    @Mock
    private ReviewerRepository repository;

    @InjectMocks
    private CreateReviewerCommandHandler sut;

    @Test
    void handle_validCommand_reviewerSaved() {
        // given - the reviewer projection carries the username shared across modules, so the persisted
        // username must match the command
        final CreateReviewerCommand command = new CreateReviewerCommand("reviewer");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Reviewer> argument = ArgumentCaptor.forClass(Reviewer.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("username", "reviewer");
    }

    @Test
    void handle_emptyUsername_reviewerSavedWithEmptyUsername() {
        // given - the handler performs no validation; an empty username is persisted verbatim
        final CreateReviewerCommand command = new CreateReviewerCommand("");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Reviewer> argument = ArgumentCaptor.forClass(Reviewer.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("username", "");
    }

    @Test
    void handle_nullUsername_reviewerSavedWithNullUsername() {
        // given - a null username is forwarded verbatim to the persisted reviewer
        final CreateReviewerCommand command = new CreateReviewerCommand(null);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Reviewer> argument = ArgumentCaptor.forClass(Reviewer.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("username", null);
    }

    @Test
    void handle_repositoryThrows_exceptionPropagated() {
        // given - a persistence failure must propagate rather than be swallowed
        final CreateReviewerCommand command = new CreateReviewerCommand("reviewer");
        doThrow(new RuntimeException("reviewer could not be saved"))
                .when(repository).save(any(Reviewer.class));

        // when / then
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("reviewer could not be saved");
    }
}
