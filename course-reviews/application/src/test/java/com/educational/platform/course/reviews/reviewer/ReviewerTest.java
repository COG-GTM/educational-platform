package com.educational.platform.course.reviews.reviewer;

import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewerTest {

    @Test
    void constructor_validCommand_usernameSet() {
        // given
        final CreateReviewerCommand command = new CreateReviewerCommand("testuser");

        // when
        final Reviewer reviewer = new Reviewer(command);

        // then
        assertThat(reviewer)
                .hasFieldOrPropertyWithValue("username", "testuser");
    }

    @Test
    void getId_beforePersistence_returnsNull() {
        // given
        final CreateReviewerCommand command = new CreateReviewerCommand("testuser");

        // when
        final Reviewer reviewer = new Reviewer(command);

        // then
        assertThat(reviewer.getId()).isNull();
    }
}
