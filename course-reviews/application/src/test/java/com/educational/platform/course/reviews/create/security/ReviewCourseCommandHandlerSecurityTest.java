package com.educational.platform.course.reviews.create.security;

import com.educational.platform.course.reviews.Comment;
import com.educational.platform.course.reviews.CourseRating;
import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewRepository;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.create.ReviewCourseCommandHandler;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommandHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the create use case's optimistic-lock lifecycle on the fully assembled application. The create handler is
 * the origin of the {@code @Version} this PR added: a successful create is the single persist that materialises
 * the row Hibernate initialises to version 0. Every other proof of that 0 origin is either a {@code @DataJpaTest}
 * slice (where {@code CurrentUserAsReviewer} is mocked and the whole test runs in one transaction) or the web API
 * test (which cannot read the internal version back). This test closes that gap: it drives the real
 * {@link ReviewCourseCommandHandler} through the real Spring context, with the real {@code CurrentUserAsReviewer}
 * resolving the {@link WithMockUser} security context and a committed transaction, and asserts the freshly created
 * row starts at version 0 - the create-side counterpart of
 * {@code UpdateCourseReviewCommandHandlerSecurityTest.handle_userIsReviewer_versionIncrementedFromSeededZero}.
 */
@Sql(scripts = "classpath:course_review.sql")
@SpringBootTest(properties = "com.educational.platform.security.enabled=true")
public class ReviewCourseCommandHandlerSecurityTest {

    // the original course uuid the seed fixture exposes as a reviewable course (course_review.sql)
    private final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

    @Autowired
    private ReviewCourseCommandHandler sut;

    @Autowired
    private UpdateCourseReviewCommandHandler updateHandler;

    @Autowired
    private CourseReviewRepository repository;

    @Test
    @WithMockUser(username = "reviewer", roles = "STUDENT")
    void handle_userIsReviewer_createdReviewVersionInitializedToZero() {
        // given - an authenticated student reviewing the seeded course
        final ReviewCourseCommand command = new ReviewCourseCommand(courseUuid, 3.0, "created comment");

        // when - the review is created through the fully assembled application: the real CurrentUserAsReviewer
        // resolves the current user against the reviewer table, the factory builds the aggregate and the handler
        // persists it in its own committed transaction
        final UUID createdUuid = sut.handle(command);

        // then - the @Version added by this PR owns the value end to end: the brand new row Hibernate just
        // inserted starts at version 0 (no seed supplied it), confirming the create flow needed no version
        // handling. The slice test mocks CurrentUserAsReviewer in a single transaction and the API test cannot
        // read the version back, so this is the only assertion of the 0 origin on the assembled application.
        final CourseReview created = repository.findByUuid(createdUuid).orElseThrow();
        assertThat(ReflectionTestUtils.getField(created, "version")).isEqualTo(0);
        assertThat(created)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(3.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("created comment"));
    }

    @Test
    @WithMockUser(username = "reviewer", roles = "STUDENT")
    void handle_userIsReviewerCreatesThenUpdates_versionProgressesZeroToOne() {
        // given - the same reviewer creates a brand new review through the assembled application; this test class
        // is not @Transactional, so the create commits in its own transaction and starts at version 0
        final UUID createdUuid = sut.handle(new ReviewCourseCommand(courseUuid, 3.0, "created comment"));
        final CourseReview afterCreate = repository.findByUuid(createdUuid).orElseThrow();
        assertThat(ReflectionTestUtils.getField(afterCreate, "version")).isEqualTo(0);

        // when - the author updates that same review through the real update use case (authorization passes: the
        // creator is the review's reviewer), in a second committed transaction
        updateHandler.handle(new UpdateCourseReviewCommand(createdUuid, 5.0, "updated comment"));

        // then - the version progresses 0 -> 1 across the create and update use cases on a row that originated
        // through the application, and the update's values are the ones persisted. The assembled-app twice-update
        // test advances a seeded row, and the slice test mocks the reviewer lookup inside a single transaction;
        // this is the only end-to-end proof of the create -> update version lifecycle on an application-created row.
        final CourseReview afterUpdate = repository.findByUuid(createdUuid).orElseThrow();
        assertThat(ReflectionTestUtils.getField(afterUpdate, "version")).isEqualTo(1);
        assertThat(afterUpdate)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("updated comment"));
    }
}
