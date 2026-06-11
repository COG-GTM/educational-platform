package com.educational.platform.course.reviews.reviewer.create;

import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.ReviewerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateReviewerCommandHandlerTest {

    @Mock
    private ReviewerRepository reviewerRepository;

    private CreateReviewerCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new CreateReviewerCommandHandler(reviewerRepository);
    }

    @Test
    void handle_validCommand_reviewerSaved() {
        // given
        final CreateReviewerCommand command = new CreateReviewerCommand("username");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Reviewer> argument = ArgumentCaptor.forClass(Reviewer.class);
        verify(reviewerRepository).save(argument.capture());
        assertThat(argument.getValue())
                .isNotNull()
                .hasFieldOrPropertyWithValue("username", "username");
    }

    @Test
    void handle_emptyUsername_reviewerSavedWithEmptyUsername() {
        // given
        final CreateReviewerCommand command = new CreateReviewerCommand("");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Reviewer> argument = ArgumentCaptor.forClass(Reviewer.class);
        verify(reviewerRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("username", "");
    }

    @Test
    void handle_validCommand_saveCalledExactlyOnce() {
        // given
        final CreateReviewerCommand command = new CreateReviewerCommand("username");

        // when
        sut.handle(command);

        // then
        verify(reviewerRepository, org.mockito.Mockito.times(1)).save(org.mockito.ArgumentMatchers.any(Reviewer.class));
    }

    @Test
    void handle_nullUsername_reviewerSavedWithNullUsername() {
        // given
        final CreateReviewerCommand command = new CreateReviewerCommand(null);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Reviewer> argument = ArgumentCaptor.forClass(Reviewer.class);
        verify(reviewerRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("username", null);
    }

    @Test
    void handle_repositorySaveThrows_exceptionPropagates() {
        // given
        final CreateReviewerCommand command = new CreateReviewerCommand("username");
        when(reviewerRepository.save(org.mockito.ArgumentMatchers.any(Reviewer.class)))
                .thenThrow(new RuntimeException("db error"));

        // when/then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db error");
    }
}
