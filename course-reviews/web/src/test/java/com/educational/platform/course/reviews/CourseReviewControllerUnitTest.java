package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.create.ReviewCourseCommandHandler;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommandHandler;
import com.educational.platform.course.reviews.query.ListCourseReviewsByCourseUUIDQuery;
import com.educational.platform.course.reviews.query.ListCourseReviewsByCourseUUIDQueryHandler;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseReviewControllerUnitTest {

    @Mock
    private ReviewCourseCommandHandler reviewHandler;

    @Mock
    private UpdateCourseReviewCommandHandler updateHandler;

    @Mock
    private ListCourseReviewsByCourseUUIDQueryHandler listHandler;

    private CourseReviewController sut;

    @BeforeEach
    void setUp() {
        sut = new CourseReviewController(reviewHandler, updateHandler, listHandler);
    }

    @Test
    void review_delegatesToReviewHandler_returnsCreatedResponse() {
        // given
        final UUID courseUuid = UUID.randomUUID();
        final UUID reviewUuid = UUID.randomUUID();
        when(reviewHandler.handle(any())).thenReturn(reviewUuid);
        final ReviewCourseRequest request = new ReviewCourseRequest(4.5, "Great course");

        // when
        final CourseReviewCreatedResponse result = sut.review(courseUuid, request);

        // then
        assertThat(result.uuid()).isEqualTo(reviewUuid);
        final ArgumentCaptor<ReviewCourseCommand> captor = ArgumentCaptor.forClass(ReviewCourseCommand.class);
        verify(reviewHandler).handle(captor.capture());
        assertThat(captor.getValue().courseId()).isEqualTo(courseUuid);
        assertThat(captor.getValue().rating()).isEqualTo(4.5);
        assertThat(captor.getValue().comment()).isEqualTo("Great course");
    }

    @Test
    void reviews_delegatesToListHandler_returnsDTOs() {
        // given
        final UUID courseUuid = UUID.randomUUID();
        final CourseReviewDTO dto = new CourseReviewDTO(UUID.randomUUID(), courseUuid, "reviewer", "Nice", 4.0);
        when(listHandler.handle(any())).thenReturn(List.of(dto));

        // when
        final List<CourseReviewDTO> result = sut.reviews(courseUuid);

        // then
        assertThat(result).containsExactly(dto);
        final ArgumentCaptor<ListCourseReviewsByCourseUUIDQuery> captor =
                ArgumentCaptor.forClass(ListCourseReviewsByCourseUUIDQuery.class);
        verify(listHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(courseUuid);
    }

    @Test
    void reviews_noReviews_returnsEmptyList() {
        // given
        final UUID courseUuid = UUID.randomUUID();
        when(listHandler.handle(any())).thenReturn(List.of());

        // when
        final List<CourseReviewDTO> result = sut.reviews(courseUuid);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void review_withNullComment_passesNullToHandler() {
        // given
        final UUID courseUuid = UUID.randomUUID();
        final UUID reviewUuid = UUID.randomUUID();
        when(reviewHandler.handle(any())).thenReturn(reviewUuid);
        final ReviewCourseRequest request = new ReviewCourseRequest(3.0, null);

        // when
        final CourseReviewCreatedResponse result = sut.review(courseUuid, request);

        // then
        assertThat(result.uuid()).isEqualTo(reviewUuid);
        final ArgumentCaptor<ReviewCourseCommand> captor = ArgumentCaptor.forClass(ReviewCourseCommand.class);
        verify(reviewHandler).handle(captor.capture());
        assertThat(captor.getValue().courseId()).isEqualTo(courseUuid);
        assertThat(captor.getValue().rating()).isEqualTo(3.0);
        assertThat(captor.getValue().comment()).isNull();
    }

    @Test
    void updateReview_delegatesToUpdateHandler() {
        // given
        final UUID courseUuid = UUID.randomUUID();
        final UUID reviewUuid = UUID.randomUUID();
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, "Updated comment");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor =
                ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(reviewUuid);
        assertThat(captor.getValue().rating()).isEqualTo(3.0);
        assertThat(captor.getValue().comment()).isEqualTo("Updated comment");
    }
}
