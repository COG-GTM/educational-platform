package com.educational.platform.courses.course.rating.update;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRating;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests boundary rating values (0.0, 5.0) for {@link UpdateCourseRatingCommandHandler}.
 */
@ExtendWith(MockitoExtension.class)
public class UpdateCourseRatingCommandHandlerBoundaryTest {

    @Mock
    private CourseRepository repository;

    private UpdateCourseRatingCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new UpdateCourseRatingCommandHandler(repository);
    }

    @Test
    void handle_zeroRating_updatesRatingToZero() {
        // given
        final UUID uuid = UUID.randomUUID();
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when
        sut.handle(new UpdateCourseRatingCommand(uuid, 0.0));

        // then
        final ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }

    @Test
    void handle_maxRating_updatesRatingToFive() {
        // given
        final UUID uuid = UUID.randomUUID();
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when
        sut.handle(new UpdateCourseRatingCommand(uuid, 5.0));

        // then
        final ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0));
    }

    @Test
    void handle_consecutiveUpdates_lastRatingWins() {
        // given
        final UUID uuid = UUID.randomUUID();
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when
        sut.handle(new UpdateCourseRatingCommand(uuid, 3.0));
        sut.handle(new UpdateCourseRatingCommand(uuid, 4.2));

        // then
        final ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.2));
    }
}
