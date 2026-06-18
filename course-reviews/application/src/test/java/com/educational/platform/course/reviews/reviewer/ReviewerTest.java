package com.educational.platform.course.reviews.reviewer;

import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewerTest {

    @Test
    void create_validCommand_reviewerCreatedWithUsername() {
        // given - the reviewer is the reviews-context projection of a user keyed by the shared username
        final CreateReviewerCommand command = new CreateReviewerCommand("reviewer");

        // when
        final Reviewer reviewer = new Reviewer(command);

        // then
        assertThat(reviewer)
                .hasFieldOrPropertyWithValue("username", "reviewer");
        assertThat(reviewer.getId()).isNull();
    }

    @Test
    void create_emptyUsername_usernameMappedVerbatim() {
        // given - the domain performs no validation; an empty username is stored as-is
        final CreateReviewerCommand command = new CreateReviewerCommand("");

        // when
        final Reviewer reviewer = new Reviewer(command);

        // then
        assertThat(reviewer)
                .hasFieldOrPropertyWithValue("username", "");
    }

    @Test
    void create_nullUsername_usernameIsNull() {
        // given - a null username is forwarded verbatim, the domain does not reject it
        final CreateReviewerCommand command = new CreateReviewerCommand(null);

        // when
        final Reviewer reviewer = new Reviewer(command);

        // then
        assertThat(reviewer)
                .hasFieldOrPropertyWithValue("username", null);
    }
}
