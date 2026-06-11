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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
}
