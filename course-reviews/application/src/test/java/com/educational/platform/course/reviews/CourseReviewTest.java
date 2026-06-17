package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-domain unit tests for the {@link CourseReview} aggregate's {@code update()} mutator - the only point in the
 * domain that mutates a persisted review. The optimistic-lock {@code version} field added by this PR is owned by
 * Hibernate, so these tests pin two invariants that protect it without a database: an edit must touch only the
 * mutable fields (rating, comment) and must leave the version field alone, and it must not disturb the identity
 * (uuid) or the foreign-key relations (course, reviewer) whose own versions the slice tests prove increment
 * independently. Every other assertion about {@code update()} runs through a JPA slice or the command handler;
 * this is the only test of the mutator in isolation, so a regression that let {@code update()} reassign a relation
 * or clobber the version would otherwise surface only indirectly.
 */
public class CourseReviewTest {

    private static final Integer COURSE = 7;
    private static final Integer REVIEWER = 9;

    @Test
    void update_changesRatingAndComment_preservesIdentityAndRelations() {
        // given - a review built through the domain constructor with known relations and identity
        final CourseReview review =
                new CourseReview(new ReviewCourseCommand(UUID.randomUUID(), 4.0, "original comment"), COURSE, REVIEWER);
        final UUID originalUuid = (UUID) ReflectionTestUtils.getField(review, "uuid");

        // when - it is edited; the command deliberately carries a different uuid than the review, so a mutator that
        // wrongly copied the command's uuid would be caught here
        review.update(new UpdateCourseReviewCommand(UUID.randomUUID(), 5.0, "edited comment"));

        // then - only the rating and comment are replaced; the uuid and the course/reviewer foreign keys (which drive
        // the listCourseReviews/isReviewer joins) survive the edit unchanged
        assertThat(review)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("edited comment"));
        assertThat(ReflectionTestUtils.getField(review, "uuid")).isEqualTo(originalUuid);
        assertThat(ReflectionTestUtils.getField(review, "course")).isEqualTo(COURSE);
        assertThat(ReflectionTestUtils.getField(review, "reviewer")).isEqualTo(REVIEWER);
    }

    @Test
    void update_doesNotManageOptimisticLockVersion() {
        // given - a transient review whose Hibernate-managed @Version is still null before any persist
        final CourseReview review =
                new CourseReview(new ReviewCourseCommand(UUID.randomUUID(), 4.0, "original comment"), COURSE, REVIEWER);
        assertThat(ReflectionTestUtils.getField(review, "version")).isNull();

        // when - the domain mutator runs
        review.update(new UpdateCourseReviewCommand(UUID.randomUUID(), 5.0, "edited comment"));

        // then - update() never assigns or increments the version: that field is Hibernate's responsibility on
        // flush, not the domain's. The slice tests prove the bump happens on persist; this proves the domain code
        // does not pre-empt or interfere with it.
        assertThat(ReflectionTestUtils.getField(review, "version")).isNull();
    }
}
