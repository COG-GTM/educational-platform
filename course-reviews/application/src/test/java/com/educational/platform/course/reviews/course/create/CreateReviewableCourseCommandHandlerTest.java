package com.educational.platform.course.reviews.course.create;

import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
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
public class CreateReviewableCourseCommandHandlerTest {

    @Mock
    private ReviewableCourseRepository reviewableCourseRepository;

    private CreateReviewableCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new CreateReviewableCourseCommandHandler(reviewableCourseRepository);
    }

    @Test
    void handle_validCommand_reviewableCourseSaved() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(courseId);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<ReviewableCourse> argument = ArgumentCaptor.forClass(ReviewableCourse.class);
        verify(reviewableCourseRepository).save(argument.capture());
        assertThat(argument.getValue()).isNotNull();
    }
}
