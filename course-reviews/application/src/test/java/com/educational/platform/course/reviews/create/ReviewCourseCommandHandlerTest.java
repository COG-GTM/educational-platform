package com.educational.platform.course.reviews.create;

import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewFactory;
import com.educational.platform.course.reviews.CourseReviewRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReviewCourseCommandHandlerTest {

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private CourseReviewFactory courseReviewFactory;

    @InjectMocks
    private ReviewCourseCommandHandler sut;

    @Test
    void handle_validCommand_reviewSavedAndUuidReturned() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

        final UUID reviewUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseReview courseReview = mock(CourseReview.class);
        when(courseReviewFactory.createFrom(command)).thenReturn(courseReview);
        when(courseReview.toIdentifier()).thenReturn(reviewUuid);

        // when
        final UUID result = sut.handle(command);

        // then
        verify(courseReviewRepository).save(courseReview);
        assertThat(result).isEqualTo(reviewUuid);
    }
}
