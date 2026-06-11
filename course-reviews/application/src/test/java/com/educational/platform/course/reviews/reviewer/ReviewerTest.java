package com.educational.platform.course.reviews.reviewer;

import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewerTest {

    @Test
    void create_validCommand_usernameStored() {
        // when
        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));

        // then
        assertThat(reviewer).hasFieldOrPropertyWithValue("username", "username");
    }

    @Test
    void getId_freshlyCreated_isNull() {
        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        assertThat(reviewer.getId()).isNull();
    }

    @Test
    void createReviewerCommand_exposesUsername() {
        assertThat(new CreateReviewerCommand("username").username()).isEqualTo("username");
    }
}
