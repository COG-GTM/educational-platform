package com.educational.platform.course.reviews.edit.security;

import com.educational.platform.course.reviews.Comment;
import com.educational.platform.course.reviews.CourseRating;
import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewRepository;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommandHandler;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.validation.ConstraintViolationException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Sql(scripts = "classpath:course_review.sql")
@SpringBootTest(properties = "com.educational.platform.security.enabled=true")
public class UpdateCourseReviewCommandHandlerSecurityTest {

    private final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Autowired
    private CourseReviewRepository repository;

    @MockitoSpyBean
    private UpdateCourseReviewCommandHandler sut;

    @Test
    @WithMockUser(username = "reviewer", roles = "STUDENT")
    void handle_userIsReviewer_courseReviewUpdated() {
        // given
        var command = new UpdateCourseReviewCommand(
                uuid, 3.0, "updated comment");

        // when
        sut.handle(command);

        // then
        final Optional<CourseReview> saved = repository.findByUuid(uuid);
        assertThat(saved.get()).hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    @WithMockUser(username = "reviewer", roles = "STUDENT")
    void handle_userIsReviewer_versionIncrementedFromSeededZero() {
        // given - the seeded review starts at version 0 (course_review.sql)
        var command = new UpdateCourseReviewCommand(uuid, 3.0, "updated comment");

        // when - the authorized reviewer updates it through the fully assembled application
        sut.handle(command);

        // then - the @Version added by this PR is bumped to 1 end-to-end through the real Spring context,
        // security authorization, command handler and JPA. Every version-increment-via-handler test is a
        // @DataJpaTest slice, so this is the only assertion that the bump happens on the assembled application.
        final CourseReview saved = repository.findByUuid(uuid).orElseThrow();
        assertThat(ReflectionTestUtils.getField(saved, "version")).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "reviewer", roles = "STUDENT")
    void handle_userIsReviewerUpdatesTwice_versionProgressesToTwo() {
        // given - the seeded review starts at version 0 (course_review.sql)
        // when - the authorized reviewer updates it twice in a row through the fully assembled application;
        // this test class is not @Transactional, so each handle call is its own committed transaction
        sut.handle(new UpdateCourseReviewCommand(uuid, 3.0, "first update"));
        sut.handle(new UpdateCourseReviewCommand(uuid, 5.0, "second update"));

        // then - each successful update advances the @Version, so it reaches 2 end-to-end, and the latest values
        // are the ones persisted. handle_userIsReviewer_versionIncrementedFromSeededZero only proves the first
        // 0 -> 1 bump on the assembled application; that repeated updates keep incrementing (1 -> 2) through the
        // real Spring context, security and JPA was only ever covered by @DataJpaTest slices until now.
        final CourseReview saved = repository.findByUuid(uuid).orElseThrow();
        assertThat(ReflectionTestUtils.getField(saved, "version")).isEqualTo(2);
        assertThat(saved)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("second update"));
    }

    @Test
    @WithMockUser(username = "reviewer", roles = "STUDENT")
    void handle_userIsReviewerInvalidRating_versionNotIncremented() {
        // given - the seeded review at version 0 (rating 4, comment "comment") and an authorized reviewer whose
        // command has a null rating, violating @NotNull
        var command = new UpdateCourseReviewCommand(uuid, null, "updated comment");

        // when - authorization passes but validation rejects the command
        final ThrowingCallable updateAction = () -> sut.handle(command);
        assertThatThrownBy(updateAction).isInstanceOf(ConstraintViolationException.class);

        // then - validation runs before the version-bumping save, so the rejected update neither persists its
        // change nor advances the optimistic-lock version: the seeded version stays 0 and the fields are unchanged.
        // handle_anotherReviewer_versionNotIncremented proves version-stays-0 for the authorization path; this proves
        // it for the validation path on the assembled application.
        final CourseReview saved = repository.findByUuid(uuid).orElseThrow();
        assertThat(ReflectionTestUtils.getField(saved, "version")).isEqualTo(0);
        assertThat(saved)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("comment"));
    }

    @Test
    @WithMockUser(username = "another-reviewer", roles = "STUDENT")
    void handle_anotherReviewer_versionNotIncremented() {
        // given - the seeded review at version 0, updated by a student who is not its author
        var command = new UpdateCourseReviewCommand(uuid, 3.0, "updated comment");

        // when - the unauthorized update is rejected (see handle_anotherReviewer_accessDeniedException)
        final ThrowingCallable updateAction = () -> sut.handle(command);
        assertThatThrownBy(updateAction).isInstanceOf(AccessDeniedException.class);

        // then - authorization runs before the version-bumping save, so the denied request neither persists
        // its change nor advances the optimistic-lock version: the seeded version stays 0
        final CourseReview saved = repository.findByUuid(uuid).orElseThrow();
        assertThat(ReflectionTestUtils.getField(saved, "version")).isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "another-reviewer", roles = "STUDENT")
    void handle_anotherReviewer_accessDeniedException() {
        // given
        var command = new UpdateCourseReviewCommand(uuid, 3.0, "updated comment");

        // when
        final ThrowingCallable updateAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(updateAction)
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void handle_userIsTeacher_accessDeniedException() {
        // given
        var command = new UpdateCourseReviewCommand(uuid, 3.0, "updated comment");

        // when
        final ThrowingCallable updateAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(updateAction)
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void handle_userIsTeacher_versionNotIncremented() {
        // given - the seeded review at version 0, updated by a teacher who is denied by the hasRole('STUDENT')
        // branch of the @PreAuthorize expression (a different authorization branch than the ownership check)
        var command = new UpdateCourseReviewCommand(uuid, 3.0, "updated comment");

        // when - the unauthorized update is rejected (see handle_userIsTeacher_accessDeniedException)
        final ThrowingCallable updateAction = () -> sut.handle(command);
        assertThatThrownBy(updateAction).isInstanceOf(AccessDeniedException.class);

        // then - authorization runs before the version-bumping save, so the denied request neither persists its
        // change nor advances the optimistic-lock version: the seeded version stays 0.
        // handle_anotherReviewer_versionNotIncremented pins this for the ownership branch (a non-author STUDENT);
        // this pins it for the role branch (a TEACHER), the only authorization denial whose version-untouched
        // guarantee was not yet asserted - handle_userIsTeacher_accessDeniedException checks only the exception.
        final CourseReview saved = repository.findByUuid(uuid).orElseThrow();
        assertThat(ReflectionTestUtils.getField(saved, "version")).isEqualTo(0);
    }
}
