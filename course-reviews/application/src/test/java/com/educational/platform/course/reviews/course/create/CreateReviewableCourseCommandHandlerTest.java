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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateReviewableCourseCommandHandlerTest {

    @Mock
    private ReviewableCourseRepository repository;

    @InjectMocks
    private CreateReviewableCourseCommandHandler sut;

    @Test
    void handle_validCommand_courseSaved() {
        // given - the reviewable-course projection is the reviews-context copy of a course keyed by the shared uuid
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(uuid);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<ReviewableCourse> argument = ArgumentCaptor.forClass(ReviewableCourse.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("originalCourseId", uuid);
    }

    @Test
    void handle_nullUuid_courseSavedWithNullOriginalCourseId() {
        // given - the handler performs no validation; a null uuid is forwarded verbatim to the persisted projection
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(null);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<ReviewableCourse> argument = ArgumentCaptor.forClass(ReviewableCourse.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("originalCourseId", null);
    }

    @Test
    void handle_repositoryThrows_exceptionPropagated() {
        // given - a persistence failure must propagate rather than be swallowed
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(uuid);
        doThrow(new RuntimeException("course could not be saved"))
                .when(repository).save(any(ReviewableCourse.class));

        // when / then
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("course could not be saved");
    }
}
