package com.educational.platform.course.reviews.reviewer;

import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link Reviewer} construction and initial state.
 */
public class ReviewerConstructionTest {

    @Test
    void constructor_setsUsername() {
        // given
        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("reviewer1"));

        // then
        assertThat(reviewer).hasFieldOrPropertyWithValue("username", "reviewer1");
    }

    @Test
    void constructor_initialIdIsNull() {
        // given
        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("reviewer2"));

        // then
        assertThat(reviewer.getId()).isNull();
    }
}
