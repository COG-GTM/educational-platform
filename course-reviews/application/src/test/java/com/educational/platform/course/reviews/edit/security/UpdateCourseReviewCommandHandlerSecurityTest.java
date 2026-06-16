package com.educational.platform.course.reviews.edit.security;

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
}
