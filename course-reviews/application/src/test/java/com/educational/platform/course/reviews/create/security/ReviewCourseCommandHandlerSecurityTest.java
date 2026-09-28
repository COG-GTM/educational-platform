package com.educational.platform.course.reviews.create.security;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.common.exception.UnprocessableEntityException;
import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewRepository;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.create.ReviewCourseCommandHandler;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;

@Sql(scripts = "classpath:course_review.sql")
@SpringBootTest(properties = "com.educational.platform.security.enabled=true")
public class ReviewCourseCommandHandlerSecurityTest {

    private final UUID reviewedCourseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
    private final UUID notReviewedCourseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

    @MockitoSpyBean
    private CourseReviewRepository repository;

    @MockitoSpyBean
    private ReviewCourseCommandHandler sut;

    @Test
    @WithMockUser(username = "reviewer", roles = "STUDENT")
    void handle_enrolledStudent_courseReviewCreated() {
        // given
        var command = new ReviewCourseCommand(notReviewedCourseUuid, 4.0, "comment");

        // when
        final UUID uuid = sut.handle(command);

        // then
        final Optional<CourseReview> saved = repository.findByUuid(uuid);
        assertThat(saved).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "reviewer", roles = "TEACHER")
    void handle_userIsTeacher_accessDeniedException() {
        // given
        var command = new ReviewCourseCommand(notReviewedCourseUuid, 4.0, "comment");

        // when
        final ThrowingCallable reviewAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(reviewAction)
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "another-reviewer", roles = "STUDENT")
    void handle_studentNotEnrolled_accessDeniedException() {
        // given
        var command = new ReviewCourseCommand(notReviewedCourseUuid, 4.0, "comment");

        // when
        final ThrowingCallable reviewAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(reviewAction)
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "reviewer", roles = "STUDENT")
    void handle_courseAlreadyReviewed_unprocessableEntityException() {
        // given
        var command = new ReviewCourseCommand(reviewedCourseUuid, 4.0, "comment");

        // when
        final ThrowingCallable reviewAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(reviewAction)
                .isInstanceOf(UnprocessableEntityException.class);
        assertThat(repository.listCourseReviews(reviewedCourseUuid)).hasSize(1);
    }

    @Test
    @WithMockUser(username = "reviewer", roles = "STUDENT")
    void handle_courseReviewedConcurrently_unprocessableEntityException() {
        // given
        doReturn(false).when(repository).existsByCourseAndReviewer(anyInt(), anyInt());
        var command = new ReviewCourseCommand(reviewedCourseUuid, 4.0, "comment");

        // when
        final ThrowingCallable reviewAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(reviewAction)
                .isInstanceOf(UnprocessableEntityException.class);
        assertThat(repository.listCourseReviews(reviewedCourseUuid)).hasSize(1);
    }

    @Test
    @WithMockUser(username = "not-replicated-reviewer", roles = "STUDENT")
    void handle_reviewerNotReplicated_relatedResourceIsNotResolvedException() {
        // given
        var command = new ReviewCourseCommand(notReviewedCourseUuid, 4.0, "comment");

        // when
        final ThrowingCallable reviewAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(reviewAction)
                .isInstanceOf(RelatedResourceIsNotResolvedException.class);
    }
}
