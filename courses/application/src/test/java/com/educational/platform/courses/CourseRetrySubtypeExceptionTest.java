package com.educational.platform.courses;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.NumberOfStudents;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommand;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;

import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.JpaOptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that the retry advice fires on <em>subclasses</em> of the configured
 * {@code retryFor = ObjectOptimisticLockingFailureException.class}, not just on instances of that
 * exact class.
 *
 * <p>This matters because the type a real stale JPA flush surfaces is not the base
 * {@link ObjectOptimisticLockingFailureException} the existing retry tests all throw by hand, but
 * {@link JpaOptimisticLockingFailureException} - the subclass Spring's persistence-exception
 * translation wraps a {@link OptimisticLockException} into. Spring Retry matches {@code retryFor} by
 * assignability, so the production code retries that subclass too; every other retry test stubs the
 * exact base class and therefore leaves this subtype path unproven. A regression that narrowed the
 * match to the exact configured class (e.g. an over-zealous custom classifier) would silently stop
 * retrying the very exception production actually throws, yet keep all the existing tests green.
 */
@SpringJUnitConfig(classes = {CourseRetryConfiguration.class, CourseRetrySubtypeExceptionTest.HandlersConfig.class})
class CourseRetrySubtypeExceptionTest {

    @Configuration
    static class HandlersConfig {

        @Bean
        CourseRepository courseRepository() {
            return mock(CourseRepository.class);
        }

        @Bean
        IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler(CourseRepository repository) {
            return new IncreaseNumberOfStudentsCommandHandler(repository);
        }

        @Bean
        UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler(CourseRepository repository) {
            return new UpdateCourseRatingCommandHandler(repository);
        }
    }

    private final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Autowired
    private CourseRepository repository;

    @Autowired
    private IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    @Autowired
    private UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    @BeforeEach
    void resetMock() {
        reset(repository);
    }

    @Test
    void increaseNumberOfStudentsHandler_retriesOnJpaSubclassOfConfiguredException_thenPersists() {
        // given - the first save clashes with the JpaOptimisticLockingFailureException subclass that
        // Spring's exception translation actually produces (not the base type the other tests stub)
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new JpaOptimisticLockingFailureException(new OptimisticLockException("stale")))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        increaseNumberOfStudentsCommandHandler.handle(new IncreaseNumberOfStudentsCommand(uuid));

        // then - retryFor matches by assignability, so the subclass is retried and the increment persists
        final ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(repository, times(2)).findByUuid(uuid);
        verify(repository, times(2)).save(saved.capture());
        assertThat(saved.getValue()).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
    }

    @Test
    void updateCourseRatingHandler_retriesOnJpaSubclassOfConfiguredException_thenPersists() {
        // given - the first save clashes with the JpaOptimisticLockingFailureException subclass that
        // Spring's exception translation actually produces (not the base type the other tests stub)
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new JpaOptimisticLockingFailureException(new OptimisticLockException("stale")))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(uuid, 3.2));

        // then - retryFor matches by assignability, so the subclass is retried and the rating persists
        verify(repository, times(2)).findByUuid(uuid);
        verify(repository, times(2)).save(any(Course.class));
    }

    @Test
    void increaseNumberOfStudentsHandler_persistentJpaSubclassFailure_exhaustsAttemptsAndRethrowsSubclass() {
        // given - every save clashes with the subclass exception
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new JpaOptimisticLockingFailureException(new OptimisticLockException("stale")));

        // when
        final IncreaseNumberOfStudentsCommand command = new IncreaseNumberOfStudentsCommand(uuid);

        // then - the subclass is retried up to maxAttempts=3, then the original subclass instance is
        // rethrown unwrapped (the retry exhaustion must not erase or re-wrap the concrete failure type)
        assertThatExceptionOfType(JpaOptimisticLockingFailureException.class)
                .isThrownBy(() -> increaseNumberOfStudentsCommandHandler.handle(command));
        verify(repository, times(3)).findByUuid(uuid);
        verify(repository, times(3)).save(any(Course.class));
    }

    @Test
    void updateCourseRatingHandler_persistentJpaSubclassFailure_exhaustsAttemptsAndRethrowsSubclass() {
        // given - every save clashes with the subclass exception
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new JpaOptimisticLockingFailureException(new OptimisticLockException("stale")));

        // when
        final UpdateCourseRatingCommand command = new UpdateCourseRatingCommand(uuid, 3.2);

        // then - the subclass is retried up to maxAttempts=3, then the original subclass instance is
        // rethrown unwrapped (the retry exhaustion must not erase or re-wrap the concrete failure type)
        assertThatExceptionOfType(JpaOptimisticLockingFailureException.class)
                .isThrownBy(() -> updateCourseRatingCommandHandler.handle(command));
        verify(repository, times(3)).findByUuid(uuid);
        verify(repository, times(3)).save(any(Course.class));
    }

    private static Course newCourse() {
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        return new Course(createCourseCommand, 15);
    }
}
