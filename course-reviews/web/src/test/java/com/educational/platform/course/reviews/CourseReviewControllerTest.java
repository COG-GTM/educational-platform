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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseReviewControllerTest {

    @Mock
    private ReviewCourseCommandHandler reviewCourseCommandHandler;

    @Mock
    private UpdateCourseReviewCommandHandler updateCourseReviewCommandHandler;

    @Mock
    private ListCourseReviewsByCourseUUIDQueryHandler listCourseReviewsByCourseUUIDQueryHandler;

    @InjectMocks
    private CourseReviewController sut;

    @Test
    void review_validRequest_returnsCreatedResponse() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "comment");
        final UUID expectedUuid = UUID.randomUUID();
        when(reviewCourseCommandHandler.handle(any(ReviewCourseCommand.class))).thenReturn(expectedUuid);

        // when
        final CourseReviewCreatedResponse result = sut.review(courseUuid, request);

        // then
        assertThat(result.uuid()).isEqualTo(expectedUuid);
        final ArgumentCaptor<ReviewCourseCommand> argument = ArgumentCaptor.forClass(ReviewCourseCommand.class);
        verify(reviewCourseCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().courseId()).isEqualTo(courseUuid);
        assertThat(argument.getValue().rating()).isEqualTo(4.0);
        assertThat(argument.getValue().comment()).isEqualTo("comment");
    }

    @Test
    void reviews_validRequest_returnsListOfReviews() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseReviewDTO dto = new CourseReviewDTO(UUID.randomUUID(), courseUuid, "username", "comment", 4.0);
        when(listCourseReviewsByCourseUUIDQueryHandler.handle(any(ListCourseReviewsByCourseUUIDQuery.class)))
                .thenReturn(List.of(dto));

        // when
        final List<CourseReviewDTO> result = sut.reviews(courseUuid);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(dto);
    }

    @Test
    void updateReview_validRequest_delegatesToHandler() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.5, "updated comment");

        // when
        sut.updateReview(courseUuid, reviewUuid, request);

        // then
        final ArgumentCaptor<UpdateCourseReviewCommand> argument = ArgumentCaptor.forClass(UpdateCourseReviewCommand.class);
        verify(updateCourseReviewCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().uuid()).isEqualTo(reviewUuid);
        assertThat(argument.getValue().rating()).isEqualTo(3.5);
        assertThat(argument.getValue().comment()).isEqualTo("updated comment");
    }
}
