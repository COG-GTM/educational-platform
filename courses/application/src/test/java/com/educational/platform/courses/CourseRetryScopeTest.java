package com.educational.platform.courses;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.approve.ApproveCourseCommand;
import com.educational.platform.courses.course.approve.ApproveCourseCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Arrays;
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
 * Pins the <em>scope</em> of the retry feature: the production {@link CourseRetryConfiguration}
 * ({@code @EnableRetry}) must apply retry advice to exactly the two {@code @Retryable} course command
 * handlers and to nothing else.
 *
 * <p>Every other retry test in this suite inspects only the two handlers the PR intentionally
 * annotated. None proves the blast radius is contained, yet {@code @EnableRetry} installs an
 * application-wide retry advisor. {@link com.educational.platform.courses.course.approve.ApproveCourseCommandHandler}
 * is the ideal control: it performs the identical {@code load -> mutate -> save} on a {@code Course},
 * is likewise {@code @Component @Transactional}, but is deliberately left without {@code @Retryable}.
 * If a regression widened the feature (moving {@code @Retryable} to a shared base, a stray global
 * retry interceptor, or {@code @EnableRetry} being misconfigured to advise all transactional beans),
 * this sibling would silently start retrying on an optimistic-lock conflict. These tests wire the
 * real {@code @EnableRetry} configuration alongside both retryable handlers and the sibling, then
 * assert the sibling neither carries the annotation, nor is wrapped in retry advice, nor retries at
 * runtime - while the two intended handlers are proxied.
 */
@SpringJUnitConfig(classes = {CourseRetryConfiguration.class, CourseRetryScopeTest.HandlersConfig.class})
class CourseRetryScopeTest {

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

        @Bean
        ApproveCourseCommandHandler approveCourseCommandHandler(CourseRepository repository) {
            return new ApproveCourseCommandHandler(repository);
        }
    }

    private final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Autowired
    private CourseRepository repository;

    @Autowired
    private IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    @Autowired
    private UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    @Autowired
    private ApproveCourseCommandHandler approveCourseCommandHandler;

    @BeforeEach
    void resetMock() {
        reset(repository);
    }

    @Test
    void approveCourseHandler_handleMethod_isNotAnnotatedRetryable() {
        // the sibling shares the load -> mutate -> save shape but must stay outside the retry contract;
        // neither the method nor the declaring class may carry @Retryable
        final boolean methodAnnotated = Arrays.stream(ApproveCourseCommandHandler.class.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(Retryable.class));
        assertThat(methodAnnotated).as("ApproveCourseCommandHandler.handle must not be @Retryable").isFalse();
        assertThat(ApproveCourseCommandHandler.class.getAnnotation(Retryable.class))
                .as("ApproveCourseCommandHandler class must not be @Retryable").isNull();
    }

    @Test
    void enableRetry_advisesOnlyRetryableHandlers_leavingTheSiblingUnproxied() {
        // @EnableRetry must scope its advisor to @Retryable beans: the two intended handlers are
        // proxied, the sibling that lacks the annotation is left as a plain (unadvised) bean
        assertThat(AopUtils.isAopProxy(increaseNumberOfStudentsCommandHandler)).isTrue();
        assertThat(AopUtils.isAopProxy(updateCourseRatingCommandHandler)).isTrue();
        assertThat(AopUtils.isAopProxy(approveCourseCommandHandler))
                .as("a sibling handler without @Retryable must not be wrapped in retry advice")
                .isFalse();
    }

    @Test
    void approveCourseHandler_optimisticLockFailureOnSave_isNotRetried() {
        // given - the sibling's save clashes on the version, the same failure the two handlers retry on
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1));

        // when
        final ApproveCourseCommand command = new ApproveCourseCommand(uuid);

        // then - lacking @Retryable, the conflict surfaces on the first attempt with no re-invocation,
        // unlike the two retrying handlers that would burn maxAttempts=3 on the identical failure
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> approveCourseCommandHandler.handle(command));
        verify(repository, times(1)).findByUuid(uuid);
        verify(repository, times(1)).save(any(Course.class));
    }

    private static Course newCourse() {
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        return new Course(createCourseCommand, 15);
    }
}
