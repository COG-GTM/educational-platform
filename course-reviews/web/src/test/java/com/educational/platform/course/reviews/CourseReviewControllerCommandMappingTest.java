package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.create.ReviewCourseCommandHandler;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommandHandler;
import com.educational.platform.course.reviews.query.ListCourseReviewsByCourseUUIDQuery;
import com.educational.platform.course.reviews.query.ListCourseReviewsByCourseUUIDQueryHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseReviewControllerCommandMappingTest {

    @Mock
    private ReviewCourseCommandHandler reviewCourseCommandHandler;

    @Mock
    private UpdateCourseReviewCommandHandler updateCourseReviewCommandHandler;

    @Mock
    private ListCourseReviewsByCourseUUIDQueryHandler listCourseReviewsByCourseUUIDQueryHandler;

    @InjectMocks
    private CourseReviewController sut;

    @Test
    void review_commandContainsCourseUuidFromPath() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.5, "Great!");
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class))).thenReturn(UUID.randomUUID());

        // when
        sut.review(courseUuid, request);

        // then
        final ArgumentCaptor<ReviewCourseCommand> captor = ArgumentCaptor.forClass(ReviewCourseCommand.class);
        verify(reviewCourseCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().courseId()).isEqualTo(courseUuid);
        assertThat(captor.getValue().rating()).isEqualTo(4.5);
        assertThat(captor.getValue().comment()).isEqualTo("Great!");
    }

    @Test
    void updateReview_commandUsesReviewUuidNotCourseUuid() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(2.0, "Changed my mind");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor = ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateCourseReviewCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(reviewUuid);
        assertThat(captor.getValue().uuid()).isNotEqualTo(courseUuid);
        assertThat(captor.getValue().rating()).isEqualTo(2.0);
        assertThat(captor.getValue().comment()).isEqualTo("Changed my mind");
    }

    @Test
    void reviews_queryContainsCourseUuidFromPath() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(listCourseReviewsByCourseUUIDQueryHandler.handle(any(ListCourseReviewsByCourseUUIDQuery.class)))
                .thenReturn(java.util.Collections.emptyList());

        // when
        sut.reviews(courseUuid);

        // then
        final ArgumentCaptor<ListCourseReviewsByCourseUUIDQuery> captor =
                ArgumentCaptor.forClass(ListCourseReviewsByCourseUUIDQuery.class);
        verify(listCourseReviewsByCourseUUIDQueryHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(courseUuid);
    }
}
