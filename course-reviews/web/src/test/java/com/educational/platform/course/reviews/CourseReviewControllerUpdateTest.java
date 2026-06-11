package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.create.ReviewCourseCommandHandler;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommandHandler;
import com.educational.platform.course.reviews.query.ListCourseReviewsByCourseUUIDQueryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CourseReviewControllerUpdateTest {

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
    void updateReview_mapsReviewUuidToCommand() {
        // given
        final UUID courseUuid = UUID.randomUUID();
        final UUID reviewUuid = UUID.randomUUID();
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(4.0, "Updated");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor =
                ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(reviewUuid);
        assertThat(captor.getValue().rating()).isEqualTo(4.0);
        assertThat(captor.getValue().comment()).isEqualTo("Updated");
    }

    @Test
    void updateReview_withNullComment_passesNullToCommand() {
        // given
        final UUID courseUuid = UUID.randomUUID();
        final UUID reviewUuid = UUID.randomUUID();
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(2.5, null);

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor =
                ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(reviewUuid);
        assertThat(captor.getValue().rating()).isEqualTo(2.5);
        assertThat(captor.getValue().comment()).isNull();
    }

    @Test
    void updateReview_withMinimumRating_passesCorrectRating() {
        // given
        final UUID courseUuid = UUID.randomUUID();
        final UUID reviewUuid = UUID.randomUUID();
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(0.0, "Terrible");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor =
                ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateHandler).handle(captor.capture());
        assertThat(captor.getValue().rating()).isEqualTo(0.0);
    }

    @Test
    void updateReview_withMaximumRating_passesCorrectRating() {
        // given
        final UUID courseUuid = UUID.randomUUID();
        final UUID reviewUuid = UUID.randomUUID();
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(5.0, "Perfect");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> captor =
                ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateHandler).handle(captor.capture());
        assertThat(captor.getValue().rating()).isEqualTo(5.0);
    }
}
