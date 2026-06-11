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
import static org.mockito.Mockito.when;

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
        assertThat(argument.getValue())
                .isNotNull()
                .hasFieldOrPropertyWithValue("originalCourseId", courseId);
    }

    @Test
    void handle_validCommand_saveCalledExactlyOnce() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(courseId);

        // when
        sut.handle(command);

        // then
        verify(reviewableCourseRepository, org.mockito.Mockito.times(1)).save(org.mockito.ArgumentMatchers.any(ReviewableCourse.class));
    }

    @Test
    void handle_nullUuidCommand_reviewableCourseSavedWithNullOriginalCourseId() {
        // given
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(null);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<ReviewableCourse> argument = ArgumentCaptor.forClass(ReviewableCourse.class);
        verify(reviewableCourseRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("originalCourseId", null);
    }

    @Test
    void handle_twoDifferentCommands_separateEntitiesSaved() {
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
        verify(reviewableCourseRepository, org.mockito.Mockito.times(2)).save(argument.capture());
        assertThat(argument.getAllValues().get(0))
                .hasFieldOrPropertyWithValue("originalCourseId", courseId1);
        assertThat(argument.getAllValues().get(1))
                .hasFieldOrPropertyWithValue("originalCourseId", courseId2);
    }

    @Test
    void handle_repositorySaveThrows_exceptionPropagates() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(courseId);
        when(reviewableCourseRepository.save(org.mockito.ArgumentMatchers.any(ReviewableCourse.class)))
                .thenThrow(new RuntimeException("db error"));

        // when/then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db error");
    }
}
