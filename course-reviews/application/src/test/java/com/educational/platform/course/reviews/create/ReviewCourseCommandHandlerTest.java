package com.educational.platform.course.reviews.create;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.reviews.Comment;
import com.educational.platform.course.reviews.CourseRating;
import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewFactory;
import com.educational.platform.course.reviews.CourseReviewRepository;
import com.educational.platform.course.reviews.CurrentUserAsReviewer;
import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the create use case. Creating a review is the origin of the {@code @Version} lifecycle this
 * PR added: a successful create is the single persist that materialises the row Hibernate then initialises to
 * version 0. These tests pin that the handler performs exactly that one persist on success and performs no
 * persist at all when the create is rejected, so a rejected create can never leave a spurious version-0 row
 * behind. The resulting version value itself (0 on insert) is asserted by the JPA slice tests; here the
 * external repository is mocked so the focus is the handler's save/no-save contract.
 */
@ExtendWith(MockitoExtension.class)
public class ReviewCourseCommandHandlerTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private ReviewableCourseRepository reviewableCourseRepository;

    @Mock
    private CurrentUserAsReviewer currentUserAsReviewer;

    private ReviewCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        final CourseReviewFactory courseReviewFactory =
                new CourseReviewFactory(validator, currentUserAsReviewer, reviewableCourseRepository);
        sut = new ReviewCourseCommandHandler(courseReviewRepository, courseReviewFactory);
    }

    @Test
    void handle_validCommand_reviewPersistedAndIdentifierReturned() {
        // given - the related course and current reviewer both resolve, so the factory can build the review
        configureRelatedResources();
        final ReviewCourseCommand command = new ReviewCourseCommand(COURSE_ID, 4.0, "comment");

        // when
        final UUID identifier = sut.handle(command);

        // then - the handler persists exactly one review (the insert that starts its optimistic-lock version
        // lifecycle) carrying the command's values, and returns that review's identifier to the caller. No
        // other test exercises the create handler in isolation: the slice tests assert the resulting version
        // is 0 and the API test drives the full stack, but neither pins that the handler issues a single
        // persist of the constructed review and surfaces its uuid.
        final ArgumentCaptor<CourseReview> argument = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(argument.capture());
        final CourseReview persisted = argument.getValue();
        assertThat(persisted)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("comment"));
        assertThat(ReflectionTestUtils.getField(persisted, "uuid")).isEqualTo(identifier);
    }

    @Test
    void handle_invalidRating_noReviewPersisted() {
        // given - an otherwise valid create whose rating violates @Max(5)
        final ReviewCourseCommand command = new ReviewCourseCommand(COURSE_ID, 6.0, "comment");

        // when - the invalid create is handled
        final ThrowingCallable handle = () -> sut.handle(command);

        // then - validation rejects the command before the factory resolves any relation or the handler reaches
        // save, so the rejected create never persists a row and therefore never creates a stray version-0 row.
        // This is the create-path counterpart of the update handler's *_noVersionBumpingSavePerformed guarantee.
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verify(courseReviewRepository, never()).save(any(CourseReview.class));
    }

    @Test
    void handle_courseNotResolved_noReviewPersisted() {
        // given - the related course cannot be resolved by its original course id
        when(reviewableCourseRepository.findByOriginalCourseId(COURSE_ID)).thenReturn(Optional.empty());
        final ReviewCourseCommand command = new ReviewCourseCommand(COURSE_ID, 4.0, "comment");

        // when - the create is handled
        final ThrowingCallable handle = () -> sut.handle(command);

        // then - the unresolved-relation failure surfaces before the handler reaches save, so a create that
        // cannot be linked to a course persists nothing and leaves no version-0 row behind.
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class).isThrownBy(handle);
        verify(courseReviewRepository, never()).save(any(CourseReview.class));
    }

    private void configureRelatedResources() {
        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(COURSE_ID));
        when(reviewableCourseRepository.findByOriginalCourseId(COURSE_ID)).thenReturn(Optional.of(reviewableCourse));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);
    }
}
