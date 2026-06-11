package com.educational.platform.course.reviews.create;

import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewFactory;
import com.educational.platform.course.reviews.CourseReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private ReviewCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new ReviewCourseCommandHandler(courseReviewRepository, courseReviewFactory);
    }

    @Test
    void handle_validCommand_savesReviewAndReturnsIdentifier() {
        // given
        final UUID courseId = UUID.randomUUID();
        final UUID reviewId = UUID.randomUUID();
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");
        final CourseReview courseReview = mock(CourseReview.class);
        when(courseReviewFactory.createFrom(command)).thenReturn(courseReview);
        when(courseReview.toIdentifier()).thenReturn(reviewId);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isEqualTo(reviewId);
        verify(courseReviewRepository).save(courseReview);
    }
}
