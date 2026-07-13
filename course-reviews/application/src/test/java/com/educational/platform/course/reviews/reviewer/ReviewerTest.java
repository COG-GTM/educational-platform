package com.educational.platform.course.reviews.reviewer;

import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewerTest {

    @Test
    void constructor_fromCommand_usernameSet() {
        // given
        final CreateReviewerCommand command = new CreateReviewerCommand("username");

        // when
        final Reviewer sut = new Reviewer(command);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("username", "username");
    }

    @Test
    void getId_returnsIdentifier() {
        // given
        final Reviewer sut = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(sut, "id", 42);

        // when
        final Integer result = sut.getId();

        // then
        assertThat(result).isEqualTo(42);
    }
}
