package com.educational.platform.course.reviews.reviewer;

import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-domain unit tests for the {@link Reviewer} aggregate's construction - its only domain entry point, since a
 * reviewer exposes no mutator. The optimistic-lock {@code version} field this PR added to {@link Reviewer} is owned
 * by Hibernate, so these tests pin, without a database, that building a reviewer from its command sets only the
 * business state (username) and leaves the {@code @Version} field untouched for Hibernate to initialise on persist.
 * {@link com.educational.platform.course.reviews.CourseReviewTest} pins the equivalent invariants for the
 * {@code CourseReview} aggregate this PR also versioned; the isolated assertion for {@link Reviewer} was missing -
 * every other reviewer test reaches the field only through a JPA slice, where a constructor that pre-set or
 * mismanaged the version would be masked by Hibernate overwriting it on flush.
 */
public class ReviewerTest {

    @Test
    void createdFromCommand_copiesUsername() {
        // given/when - a reviewer built through the domain constructor from its create command
        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("reviewer-username"));

        // then - the constructor copies the command's username verbatim. Other reviewer tests only observe this
        // indirectly (a slice saves a reviewer and looks it up by username); this pins the construction contract
        // in isolation, with no Spring context or database.
        assertThat(ReflectionTestUtils.getField(reviewer, "username")).isEqualTo("reviewer-username");
    }

    @Test
    void createdFromCommand_doesNotManageOptimisticLockVersion() {
        // given/when - a transient reviewer whose Hibernate-managed @Version this PR added
        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("reviewer-username"));

        // then - construction never assigns the version: it stays null on the transient instance (the value the
        // slice tests prove Hibernate initialises to 0 on insert). This is the reviewer counterpart of
        // CourseReviewTest.update_doesNotManageOptimisticLockVersion - it proves the domain code does not pre-empt
        // or interfere with the version Hibernate owns.
        assertThat(ReflectionTestUtils.getField(reviewer, "version")).isNull();
    }
}
