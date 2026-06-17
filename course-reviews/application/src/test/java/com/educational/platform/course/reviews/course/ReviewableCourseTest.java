package com.educational.platform.course.reviews.course;

import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-domain unit tests for the {@link ReviewableCourse} aggregate's construction - its only domain entry point,
 * since a reviewable course exposes no mutator. The optimistic-lock {@code version} field this PR added to
 * {@link ReviewableCourse} is owned by Hibernate, so these tests pin, without a database, that building a reviewable
 * course from its command sets only the business state (originalCourseId) and leaves the {@code @Version} field
 * untouched for Hibernate to initialise on persist. {@link com.educational.platform.course.reviews.CourseReviewTest}
 * pins the equivalent invariants for the {@code CourseReview} aggregate this PR also versioned; the isolated
 * assertion for {@link ReviewableCourse} was missing, and unlike the other two entities it is excluded from the
 * migrated-schema slice (its {@code original_course_id} column is absent from the migrated table), so an isolated
 * domain test is the only place the constructor's hands-off treatment of the version is asserted directly.
 */
public class ReviewableCourseTest {

    @Test
    void createdFromCommand_copiesOriginalCourseId() {
        // given/when - a reviewable course built through the domain constructor from its create command
        final UUID originalCourseId = UUID.randomUUID();
        final ReviewableCourse reviewableCourse =
                new ReviewableCourse(new CreateReviewableCourseCommand(originalCourseId));

        // then - the constructor copies the command's uuid into originalCourseId verbatim. Other tests observe this
        // only indirectly (a slice saves a course and looks it up by original course id); this pins the construction
        // contract in isolation, with no Spring context or database.
        assertThat(ReflectionTestUtils.getField(reviewableCourse, "originalCourseId")).isEqualTo(originalCourseId);
    }

    @Test
    void createdFromCommand_doesNotManageOptimisticLockVersion() {
        // given/when - a transient reviewable course whose Hibernate-managed @Version this PR added
        final ReviewableCourse reviewableCourse =
                new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()));

        // then - construction never assigns the version: it stays null on the transient instance (the value the
        // slice tests prove Hibernate initialises to 0 on insert). This is the reviewable-course counterpart of
        // CourseReviewTest.update_doesNotManageOptimisticLockVersion - it proves the domain code does not pre-empt
        // or interfere with the version Hibernate owns.
        assertThat(ReflectionTestUtils.getField(reviewableCourse, "version")).isNull();
    }
}
