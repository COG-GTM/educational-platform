package com.educational.platform.course.reviews.reviewer.create;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateReviewerCommandTest {

    @Test
    void constructor_validUsername_usernameStored() {
        // when
        final CreateReviewerCommand command = new CreateReviewerCommand("testuser");

        // then
        assertThat(command.username()).isEqualTo("testuser");
    }

    @Test
    void constructor_nullUsername_nullStored() {
        // when
        final CreateReviewerCommand command = new CreateReviewerCommand(null);

        // then
        assertThat(command.username()).isNull();
    }

    @Test
    void constructor_emptyUsername_emptyStored() {
        // when
        final CreateReviewerCommand command = new CreateReviewerCommand("");

        // then
        assertThat(command.username()).isEmpty();
    }

    @Test
    void equals_sameUsername_returnsTrue() {
        // given
        final CreateReviewerCommand first = new CreateReviewerCommand("user");
        final CreateReviewerCommand second = new CreateReviewerCommand("user");

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_differentUsername_returnsFalse() {
        // given
        final CreateReviewerCommand first = new CreateReviewerCommand("user1");
        final CreateReviewerCommand second = new CreateReviewerCommand("user2");

        // then
        assertThat(first).isNotEqualTo(second);
    }
}
