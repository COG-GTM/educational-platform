package com.educational.platform.course.reviews.course.create;

import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateReviewableCourseCommandHandlerEdgeCaseTest {

    @Mock
    private ReviewableCourseRepository reviewableCourseRepository;

    @InjectMocks
    private CreateReviewableCourseCommandHandler sut;

    @Test
    void handle_validCommand_reviewableCourseSavedWithCorrectOriginalCourseId() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(courseId);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<ReviewableCourse> argument = ArgumentCaptor.forClass(ReviewableCourse.class);
        verify(reviewableCourseRepository).save(argument.capture());
        assertThat(argument.getValue()).hasFieldOrPropertyWithValue("originalCourseId", courseId);
    }

    @Test
    void handle_twoDifferentCourses_bothSaved() {
        // given
        final UUID courseId1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseId2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CreateReviewableCourseCommand command1 = new CreateReviewableCourseCommand(courseId1);
        final CreateReviewableCourseCommand command2 = new CreateReviewableCourseCommand(courseId2);

        // when
        sut.handle(command1);
        sut.handle(command2);

        // then
        final ArgumentCaptor<ReviewableCourse> argument = ArgumentCaptor.forClass(ReviewableCourse.class);
        verify(reviewableCourseRepository, times(2)).save(argument.capture());
        assertThat(argument.getAllValues()).hasSize(2);
        assertThat(argument.getAllValues().get(0)).hasFieldOrPropertyWithValue("originalCourseId", courseId1);
        assertThat(argument.getAllValues().get(1)).hasFieldOrPropertyWithValue("originalCourseId", courseId2);
    }
}
