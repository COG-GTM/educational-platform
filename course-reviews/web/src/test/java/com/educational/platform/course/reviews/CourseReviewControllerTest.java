package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.create.ReviewCourseCommandHandler;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommandHandler;
import com.educational.platform.course.reviews.query.ListCourseReviewsByCourseUUIDQueryHandler;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import com.educational.platform.course.reviews.query.ListCourseReviewsByCourseUUIDQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.common.exception.ResourceNotFoundException;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class CourseReviewControllerTest {

    @Mock
    private ReviewCourseCommandHandler reviewCourseCommandHandler;

    @Mock
    private UpdateCourseReviewCommandHandler updateCourseReviewCommandHandler;

    @Mock
    private ListCourseReviewsByCourseUUIDQueryHandler listCourseReviewsByCourseUUIDQueryHandler;

    private CourseReviewController sut;

    @BeforeEach
    void setUp() {
        sut = new CourseReviewController(reviewCourseCommandHandler, updateCourseReviewCommandHandler, listCourseReviewsByCourseUUIDQueryHandler);
    }

    @Test
    void review_validRequest_returnsCourseReviewCreatedResponse() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "great");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class))).thenReturn(reviewUuid);

        // when
        final CourseReviewCreatedResponse response = sut.review(courseUuid, request);

        // then
        assertThat(response.uuid()).isEqualTo(reviewUuid);
    }

    @Test
    void review_validRequest_delegatesWithCorrectCommand() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "great");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class))).thenReturn(reviewUuid);

        // when
        sut.review(courseUuid, request);

        // then
        final ArgumentCaptor<ReviewCourseCommand> captor = ArgumentCaptor.forClass(ReviewCourseCommand.class);
        verify(reviewCourseCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().courseId()).isEqualTo(courseUuid);
        assertThat(captor.getValue().rating()).isEqualTo(4.0);
        assertThat(captor.getValue().comment()).isEqualTo("great");
    }

    @Test
    void reviews_validUuid_returnsDTOList() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseReviewDTO dto = new CourseReviewDTO(reviewUuid, courseUuid, "user", "comment", 4.0);
        when(listCourseReviewsByCourseUUIDQueryHandler.handle(any(ListCourseReviewsByCourseUUIDQuery.class)))
                .thenReturn(List.of(dto));

        // when
        final List<CourseReviewDTO> result = sut.reviews(courseUuid);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().uuid()).isEqualTo(reviewUuid);
    }

    @Test
    void reviews_validUuid_delegatesWithCorrectQuery() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(listCourseReviewsByCourseUUIDQueryHandler.handle(any(ListCourseReviewsByCourseUUIDQuery.class)))
                .thenReturn(List.of());

        // when
        sut.reviews(courseUuid);

        // then
        final ArgumentCaptor<ListCourseReviewsByCourseUUIDQuery> captor = ArgumentCaptor.forClass(ListCourseReviewsByCourseUUIDQuery.class);
        verify(listCourseReviewsByCourseUUIDQueryHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(courseUuid);
    }

    @Test
    void reviews_emptyList_returnsEmptyList() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(listCourseReviewsByCourseUUIDQueryHandler.handle(any(ListCourseReviewsByCourseUUIDQuery.class)))
                .thenReturn(List.of());

        // when
        final List<CourseReviewDTO> result = sut.reviews(courseUuid);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void updateReview_validRequest_delegatesWithCorrectCommand() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, "updated");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor = ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateCourseReviewCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(reviewUuid);
        assertThat(captor.getValue().rating()).isEqualTo(3.0);
        assertThat(captor.getValue().comment()).isEqualTo("updated");
    }

    @Test
    void review_nullComment_delegatesWithNullComment() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, null);
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class))).thenReturn(reviewUuid);

        // when
        sut.review(courseUuid, request);

        // then
        final ArgumentCaptor<ReviewCourseCommand> captor = ArgumentCaptor.forClass(ReviewCourseCommand.class);
        verify(reviewCourseCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().comment()).isNull();
    }

    @Test
    void updateReview_nullComment_delegatesWithNullComment() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, null);

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor = ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateCourseReviewCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().comment()).isNull();
    }

    @Test
    void review_validRequest_handlerCalledExactlyOnce() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "great");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class)))
                .thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

        // when
        sut.review(courseUuid, request);

        // then
        verify(reviewCourseCommandHandler, times(1)).handle(any(ReviewCourseCommand.class));
    }

    @Test
    void updateReview_validRequest_handlerCalledExactlyOnce() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, "updated");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        verify(updateCourseReviewCommandHandler, times(1)).handle(any(UpdateCourseReviewCommand.class));
    }

    @Test
    void review_zeroRating_delegatesWithZeroRating() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(0.0, "zero");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class)))
                .thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

        // when
        sut.review(courseUuid, request);

        // then
        final ArgumentCaptor<ReviewCourseCommand> captor = ArgumentCaptor.forClass(ReviewCourseCommand.class);
        verify(reviewCourseCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().rating()).isEqualTo(0.0);
    }

    @Test
    void updateReview_zeroRating_delegatesWithZeroRating() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(0.0, "zero");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor = ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateCourseReviewCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().rating()).isEqualTo(0.0);
    }

    @Test
    void reviews_multipleReviews_allReturned() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseReviewDTO dto1 = new CourseReviewDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440002"), courseUuid, "user1", "good", 4.0);
        final CourseReviewDTO dto2 = new CourseReviewDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440003"), courseUuid, "user2", "great", 5.0);
        when(listCourseReviewsByCourseUUIDQueryHandler.handle(any(ListCourseReviewsByCourseUUIDQuery.class)))
                .thenReturn(List.of(dto1, dto2));

        // when
        final List<CourseReviewDTO> result = sut.reviews(courseUuid);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CourseReviewDTO::username).containsExactly("user1", "user2");
    }

    @Test
    void updateReview_usesReviewUuidNotCourseUuid() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(4.0, "comment");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor = ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateCourseReviewCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(reviewUuid);
        assertThat(captor.getValue().uuid()).isNotEqualTo(courseUuid);
    }

    @Test
    void review_maxRating_delegatesWithMaxRating() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(5.0, "excellent");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class)))
                .thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

        // when
        sut.review(courseUuid, request);

        // then
        final ArgumentCaptor<ReviewCourseCommand> captor = ArgumentCaptor.forClass(ReviewCourseCommand.class);
        verify(reviewCourseCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().rating()).isEqualTo(5.0);
    }

    @Test
    void updateReview_maxRating_delegatesWithMaxRating() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(5.0, "excellent");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor = ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateCourseReviewCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().rating()).isEqualTo(5.0);
    }

    @Test
    void review_emptyComment_delegatesWithEmptyComment() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class)))
                .thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

        // when
        sut.review(courseUuid, request);

        // then
        final ArgumentCaptor<ReviewCourseCommand> captor = ArgumentCaptor.forClass(ReviewCourseCommand.class);
        verify(reviewCourseCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().comment()).isEmpty();
    }

    @Test
    void updateReview_emptyComment_delegatesWithEmptyComment() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, "");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor = ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateCourseReviewCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().comment()).isEmpty();
    }

    @Test
    void reviews_queryHandlerCalledExactlyOnce() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(listCourseReviewsByCourseUUIDQueryHandler.handle(any(ListCourseReviewsByCourseUUIDQuery.class)))
                .thenReturn(List.of());

        // when
        sut.reviews(courseUuid);

        // then
        verify(listCourseReviewsByCourseUUIDQueryHandler, times(1)).handle(any(ListCourseReviewsByCourseUUIDQuery.class));
    }

    @Test
    void review_handlerThrowsRelatedResourceNotResolved_exceptionPropagates() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "comment");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class)))
                .thenThrow(new RelatedResourceIsNotResolvedException("Course not found"));

        // when/then
        assertThatThrownBy(() -> sut.review(courseUuid, request))
                .isInstanceOf(RelatedResourceIsNotResolvedException.class);
    }

    @Test
    void updateReview_handlerThrowsResourceNotFound_exceptionPropagates() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, "updated");
        doThrow(new ResourceNotFoundException("Review not found"))
                .when(updateCourseReviewCommandHandler).handle(any(UpdateCourseReviewCommand.class));

        // when/then
        assertThatThrownBy(() -> sut.updateReview(courseUuid, reviewUuid, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void review_doesNotInteractWithUpdateOrQueryHandlers() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "great");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class)))
                .thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

        // when
        sut.review(courseUuid, request);

        // then
        verifyNoInteractions(updateCourseReviewCommandHandler);
        verifyNoInteractions(listCourseReviewsByCourseUUIDQueryHandler);
    }

    @Test
    void updateReview_doesNotInteractWithReviewOrQueryHandlers() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, "updated");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        verifyNoInteractions(reviewCourseCommandHandler);
        verifyNoInteractions(listCourseReviewsByCourseUUIDQueryHandler);
    }

    @Test
    void review_fractionalRating_delegatesWithFractionalRating() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(2.5, "decent");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class)))
                .thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

        // when
        sut.review(courseUuid, request);

        // then
        final ArgumentCaptor<ReviewCourseCommand> captor = ArgumentCaptor.forClass(ReviewCourseCommand.class);
        verify(reviewCourseCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().rating()).isEqualTo(2.5);
    }

    @Test
    void updateReview_fractionalRating_delegatesWithFractionalRating() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(2.5, "decent");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor = ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateCourseReviewCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().rating()).isEqualTo(2.5);
    }

    @Test
    void review_responseContainsExactUuidFromHandler() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID expectedUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440099");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "great");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class))).thenReturn(expectedUuid);

        // when
        final CourseReviewCreatedResponse response = sut.review(courseUuid, request);

        // then
        assertThat(response.uuid()).isEqualTo(expectedUuid);
    }

    @Test
    void reviews_doesNotInteractWithReviewOrUpdateHandlers() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(listCourseReviewsByCourseUUIDQueryHandler.handle(any(ListCourseReviewsByCourseUUIDQuery.class)))
                .thenReturn(List.of());

        // when
        sut.reviews(courseUuid);

        // then
        verifyNoInteractions(reviewCourseCommandHandler);
        verifyNoInteractions(updateCourseReviewCommandHandler);
    }

    @Test
    void review_handlerThrowsConstraintViolation_exceptionPropagates() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "comment");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class)))
                .thenThrow(new ConstraintViolationException(Set.of()));

        // when/then
        assertThatThrownBy(() -> sut.review(courseUuid, request))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void updateReview_handlerThrowsConstraintViolation_exceptionPropagates() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, "updated");
        doThrow(new ConstraintViolationException(Set.of()))
                .when(updateCourseReviewCommandHandler).handle(any(UpdateCourseReviewCommand.class));

        // when/then
        assertThatThrownBy(() -> sut.updateReview(courseUuid, reviewUuid, request))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void reviews_handlerThrowsRuntimeException_exceptionPropagates() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(listCourseReviewsByCourseUUIDQueryHandler.handle(any(ListCourseReviewsByCourseUUIDQuery.class)))
                .thenThrow(new RuntimeException("unexpected"));

        // when/then
        assertThatThrownBy(() -> sut.reviews(courseUuid))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("unexpected");
    }

    @Test
    void review_handlerReturnsNull_responseContainsNullUuid() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "great");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class))).thenReturn(null);

        // when
        final CourseReviewCreatedResponse response = sut.review(courseUuid, request);

        // then
        assertThat(response.uuid()).isNull();
    }

    @Test
    void review_handlerThrowsRuntimeException_exceptionPropagates() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "comment");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class)))
                .thenThrow(new RuntimeException("unexpected"));

        // when/then
        assertThatThrownBy(() -> sut.review(courseUuid, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("unexpected");
    }

    @Test
    void updateReview_handlerThrowsRuntimeException_exceptionPropagates() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, "updated");
        doThrow(new RuntimeException("unexpected"))
                .when(updateCourseReviewCommandHandler).handle(any(UpdateCourseReviewCommand.class));

        // when/then
        assertThatThrownBy(() -> sut.updateReview(courseUuid, reviewUuid, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("unexpected");
    }

    @Test
    void reviews_singleReview_allDTOFieldsReturnedCorrectly() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseReviewDTO dto = new CourseReviewDTO(reviewUuid, courseUuid, "reviewer-user", "detailed comment", 3.7);
        when(listCourseReviewsByCourseUUIDQueryHandler.handle(any(ListCourseReviewsByCourseUUIDQuery.class)))
                .thenReturn(List.of(dto));

        // when
        final List<CourseReviewDTO> result = sut.reviews(courseUuid);

        // then
        assertThat(result).hasSize(1);
        final CourseReviewDTO returned = result.getFirst();
        assertThat(returned.uuid()).isEqualTo(reviewUuid);
        assertThat(returned.course()).isEqualTo(courseUuid);
        assertThat(returned.username()).isEqualTo("reviewer-user");
        assertThat(returned.comment()).isEqualTo("detailed comment");
        assertThat(returned.rating()).isEqualTo(3.7);
    }

    @Test
    void updateReview_handlerThrowsRelatedResourceNotResolved_exceptionPropagates() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, "updated");
        doThrow(new RelatedResourceIsNotResolvedException("Course not found"))
                .when(updateCourseReviewCommandHandler).handle(any(UpdateCourseReviewCommand.class));

        // when/then
        assertThatThrownBy(() -> sut.updateReview(courseUuid, reviewUuid, request))
                .isInstanceOf(RelatedResourceIsNotResolvedException.class);
    }

    @Test
    void reviews_nullUuid_delegatesWithNullUuid() {
        // given
        when(listCourseReviewsByCourseUUIDQueryHandler.handle(any(ListCourseReviewsByCourseUUIDQuery.class)))
                .thenReturn(List.of());

        // when
        sut.reviews(null);

        // then
        final ArgumentCaptor<ListCourseReviewsByCourseUUIDQuery> captor = ArgumentCaptor.forClass(ListCourseReviewsByCourseUUIDQuery.class);
        verify(listCourseReviewsByCourseUUIDQueryHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isNull();
    }

    @Test
    void review_nullCourseUuid_delegatesWithNullCourseUuid() {
        // given — null path variable is passed through to command
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "comment");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class)))
                .thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

        // when
        sut.review(null, request);

        // then
        final ArgumentCaptor<ReviewCourseCommand> captor = ArgumentCaptor.forClass(ReviewCourseCommand.class);
        verify(reviewCourseCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().courseId()).isNull();
    }

    @Test
    void updateReview_nullReviewUuid_delegatesWithNullUuid() {
        // given — null review UUID is passed through to command
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, "updated");

        // when
        sut.updateReview(courseUuid, null, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor = ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateCourseReviewCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isNull();
    }

    @Test
    void review_longComment_delegatesWithFullComment() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String longComment = "a".repeat(1000);
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, longComment);
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class)))
                .thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

        // when
        sut.review(courseUuid, request);

        // then
        final ArgumentCaptor<ReviewCourseCommand> captor = ArgumentCaptor.forClass(ReviewCourseCommand.class);
        verify(reviewCourseCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().comment()).isEqualTo(longComment);
    }

    @Test
    void updateReview_courseUuidNotEmbeddedInCommand() {
        // given — courseUuid and reviewUuid are different; only reviewUuid should appear in the command
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(4.0, "ok");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor = ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateCourseReviewCommandHandler).handle(captor.capture());
        final UpdateCourseReviewCommand captured = captor.getValue();
        assertThat(captured.uuid()).isEqualTo(reviewUuid);
        assertThat(captured.rating()).isEqualTo(4.0);
        assertThat(captured.comment()).isEqualTo("ok");
        // courseUuid is a path variable but is not part of the UpdateCourseReviewCommand
    }

    @Test
    void reviews_twoDifferentCourseUuids_delegatesWithCorrectUuidEachTime() {
        // given
        final UUID courseUuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        when(listCourseReviewsByCourseUUIDQueryHandler.handle(any(ListCourseReviewsByCourseUUIDQuery.class)))
                .thenReturn(List.of());

        // when
        sut.reviews(courseUuid1);
        sut.reviews(courseUuid2);

        // then
        final ArgumentCaptor<ListCourseReviewsByCourseUUIDQuery> captor =
                ArgumentCaptor.forClass(ListCourseReviewsByCourseUUIDQuery.class);
        verify(listCourseReviewsByCourseUUIDQueryHandler, times(2)).handle(captor.capture());
        assertThat(captor.getAllValues().get(0).uuid()).isEqualTo(courseUuid1);
        assertThat(captor.getAllValues().get(1).uuid()).isEqualTo(courseUuid2);
    }

    @Test
    void review_whitespaceComment_delegatesWithWhitespaceComment() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "   ");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class)))
                .thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

        // when
        sut.review(courseUuid, request);

        // then
        final ArgumentCaptor<ReviewCourseCommand> captor = ArgumentCaptor.forClass(ReviewCourseCommand.class);
        verify(reviewCourseCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().comment()).isEqualTo("   ");
    }

    @Test
    void review_responseType_isCourseReviewCreatedResponse() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "great");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class)))
                .thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

        // when
        final Object response = sut.review(courseUuid, request);

        // then
        assertThat(response).isInstanceOf(CourseReviewCreatedResponse.class);
    }

    @Test
    void updateReview_longComment_delegatesWithFullComment() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final String longComment = "b".repeat(1000);
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, longComment);

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor = ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateCourseReviewCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().comment()).isEqualTo(longComment);
    }

    @Test
    void updateReview_whitespaceComment_delegatesWithWhitespaceComment() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, "   ");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor = ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateCourseReviewCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().comment()).isEqualTo("   ");
    }

    @Test
    void updateReview_nullCourseUuid_delegatesWithOnlyReviewUuid() {
        // given — null courseUuid should not affect the command construction
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, "comment");

        // when
        sut.updateReview(null, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor = ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateCourseReviewCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(reviewUuid);
        assertThat(captor.getValue().rating()).isEqualTo(3.0);
        assertThat(captor.getValue().comment()).isEqualTo("comment");
    }

    @Test
    void reviews_handlerReturnsNull_nullReturned() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(listCourseReviewsByCourseUUIDQueryHandler.handle(any(ListCourseReviewsByCourseUUIDQuery.class)))
                .thenReturn(null);

        // when
        final List<CourseReviewDTO> result = sut.reviews(courseUuid);

        // then
        assertThat(result).isNull();
    }
}
