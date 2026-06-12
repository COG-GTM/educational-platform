package com.educational.platform.course.reviews.create;

import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewFactory;
import com.educational.platform.course.reviews.CourseReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewCourseCommandHandlerTest {

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private CourseReviewFactory courseReviewFactory;

    @InjectMocks
    private ReviewCourseCommandHandler sut;

    @Test
    void handle_validCommand_courseReviewSavedAndUuidReturned() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

        final CourseReview courseReview = mock(CourseReview.class);
        final UUID expectedUuid = UUID.randomUUID();
        when(courseReviewFactory.createFrom(command)).thenReturn(courseReview);
        when(courseReview.toIdentifier()).thenReturn(expectedUuid);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isEqualTo(expectedUuid);
        verify(courseReviewRepository).save(courseReview);
    }
}
