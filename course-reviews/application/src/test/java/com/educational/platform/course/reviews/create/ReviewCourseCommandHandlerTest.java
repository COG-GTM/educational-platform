package com.educational.platform.course.reviews.create;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.common.exception.UnprocessableEntityException;
import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewFactory;
import com.educational.platform.course.reviews.CourseReviewRepository;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReviewCourseCommandHandlerTest {

    private final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
    private final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private CourseReviewFactory courseReviewFactory;

    @InjectMocks
    private ReviewCourseCommandHandler sut;

    private CourseReview courseReview;

    @BeforeEach
    void setUp() {
        courseReview = mock(CourseReview.class);
        when(courseReviewFactory.createFrom(command)).thenReturn(courseReview);
    }

    @Test
    void handle_validCommand_reviewFlushedAndUuidReturned() {
        // given
        when(courseReview.toIdentifier()).thenReturn(reviewUuid);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isEqualTo(reviewUuid);
        verify(courseReviewRepository).saveAndFlush(courseReview);
    }

    @Test
    void handle_reviewerCourseUniqueViolation_unprocessableEntityException() {
        // given
        final SQLException cause = new SQLException("Unique index or primary key violation: \"PUBLIC.COURSE_REVIEW_REVIEWER_COURSE_UK ON PUBLIC.COURSE_REVIEW(REVIEWER, COURSE)\"");
        when(courseReviewRepository.saveAndFlush(courseReview))
                .thenThrow(new DataIntegrityViolationException("could not execute statement", cause));

        // when
        final ThrowingCallable handleAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(handleAction)
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining(courseId.toString());
    }

    @Test
    void handle_unrelatedIntegrityViolation_originalExceptionRethrown() {
        // given
        final DataIntegrityViolationException original = new DataIntegrityViolationException("could not execute statement",
                new SQLException("NULL not allowed for column \"RATING\""));
        when(courseReviewRepository.saveAndFlush(courseReview)).thenThrow(original);

        // when
        final ThrowingCallable handleAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(handleAction).isSameAs(original);
    }

    @Test
    void handle_integrityViolationWithoutMessage_originalExceptionRethrown() {
        // given
        final DataIntegrityViolationException original = new DataIntegrityViolationException(null);
        when(courseReviewRepository.saveAndFlush(courseReview)).thenThrow(original);

        // when
        final ThrowingCallable handleAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(handleAction).isSameAs(original);
    }

    @Test
    void handle_reviewerCourseUniqueViolationLowerCaseConstraintName_unprocessableEntityException() {
        // given
        final SQLException cause = new SQLException("ERROR: duplicate key value violates unique constraint \"course_review_reviewer_course_uk\"");
        when(courseReviewRepository.saveAndFlush(courseReview))
                .thenThrow(new DataIntegrityViolationException("could not execute statement", cause));

        // when
        final ThrowingCallable handleAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(handleAction)
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining(courseId.toString());
    }

    @Test
    void handle_factoryRejectsCommand_nothingSavedAndExceptionPropagated() {
        // given
        final RelatedResourceIsNotResolvedException factoryException = new RelatedResourceIsNotResolvedException("Course cannot be found");
        when(courseReviewFactory.createFrom(command)).thenThrow(factoryException);

        // when
        final ThrowingCallable handleAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(handleAction).isSameAs(factoryException);
        verify(courseReviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void handle_reviewerCourseUniqueViolationNestedInHibernateException_unprocessableEntityException() {
        // given
        final SQLException root = new SQLException("Unique index or primary key violation: \"PUBLIC.COURSE_REVIEW_REVIEWER_COURSE_UK\"");
        final ConstraintViolationException hibernateCause = new ConstraintViolationException("could not execute statement", root, "course_review_reviewer_course_uk");
        when(courseReviewRepository.saveAndFlush(courseReview))
                .thenThrow(new DataIntegrityViolationException("could not execute statement", hibernateCause));

        // when
        final ThrowingCallable handleAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(handleAction)
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining(courseId.toString());
    }

    @Test
    void handle_constraintNameOnlyInIntermediateCause_originalExceptionRethrown() {
        // given
        final SQLException root = new SQLException("NULL not allowed for column \"RATING\"");
        final ConstraintViolationException hibernateCause = new ConstraintViolationException("violates course_review_reviewer_course_uk", root, "course_review_reviewer_course_uk");
        final DataIntegrityViolationException original = new DataIntegrityViolationException("could not execute statement", hibernateCause);
        when(courseReviewRepository.saveAndFlush(courseReview)).thenThrow(original);

        // when
        final ThrowingCallable handleAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(handleAction).isSameAs(original);
    }

    @Test
    void handle_reviewerCourseUniqueViolationWithoutCause_unprocessableEntityException() {
        // given
        when(courseReviewRepository.saveAndFlush(courseReview))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"course_review_reviewer_course_uk\""));

        // when
        final ThrowingCallable handleAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(handleAction).isInstanceOf(UnprocessableEntityException.class);
    }

}
