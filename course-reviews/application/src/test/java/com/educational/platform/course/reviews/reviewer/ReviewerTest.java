package com.educational.platform.course.reviews.reviewer;

import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewerTest {

    @Test
    void constructor_validCommand_reviewerCreated() {
        // given
        final CreateReviewerCommand command = new CreateReviewerCommand("username");

        // when
        final Reviewer result = new Reviewer(command);

        // then
        assertThat(result).hasFieldOrPropertyWithValue("username", "username");
    }

    @Test
    void getId_afterSettingId_returnsId() {
        // given
        final CreateReviewerCommand command = new CreateReviewerCommand("username");
        final Reviewer sut = new Reviewer(command);
        ReflectionTestUtils.setField(sut, "id", 42);

        // when
        final Integer id = sut.getId();

        // then
        assertThat(id).isEqualTo(42);
    }
}
