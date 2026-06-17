package com.educational.platform.courses;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.approve.ApproveCourseCommand;
import com.educational.platform.courses.course.approve.ApproveCourseCommandHandler;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;

import org.aopalliance.aop.Advice;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that the {@code @EnableRetry} declared by the component-scanned
 * {@link CourseRetryConfiguration} is contained to the two {@code @Retryable} course command handlers
 * and does <em>not</em> leak retry advice onto sibling {@code @Component @Transactional} command
 * handlers in the <em>real</em> application context.
 *
 * <p>{@code CourseRetryScopeTest} proves the same containment, but only inside a sliced
 * {@code @SpringJUnitConfig} context that hand-declares four beans and has no transaction management,
 * so the sibling handler is left entirely unproxied there. {@code CourseRetryApplicationContextWiringTest}
 * boots the full context but only asserts the two target handlers <em>are</em> retry-aware - it never
 * checks that the siblings are <em>not</em>. This test closes that omission by booting the production
 * component scan (where the sibling is a CGLIB proxy carrying transaction advice) and asserting, both
 * structurally and behaviourally, that the {@code @EnableRetry} advisor was not applied to it. It
 * guards against a scan-driven widening of the retry blast radius (e.g. {@code @Retryable} migrating
 * onto a shared base type) that the sliced four-bean test cannot observe.
 */
@SpringBootTest
class CourseRetryProductionScopeContainmentTest {

    private final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @MockitoBean
    private CourseRepository repository;

    @Autowired
    private ApproveCourseCommandHandler approveCourseCommandHandler;

    @Autowired
    private IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    @Autowired
    private UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    @Test
    void productionContext_siblingTransactionalHandler_carriesTransactionAdviceButNoRetryAdvice() {
        // the sibling is @Transactional, so it must still be an AOP proxy carrying transaction advice...
        assertThat(AopUtils.isAopProxy(approveCourseCommandHandler)).isTrue();
        assertThat(hasTransactionAdvice(approveCourseCommandHandler))
                .as("sibling command handler keeps its @Transactional advice").isTrue();

        // ...but the @EnableRetry advisor must not have widened onto it
        assertThat(hasRetryAdvice(approveCourseCommandHandler))
                .as("@EnableRetry must not advise the non-@Retryable sibling handler").isFalse();

        // sanity contrast: the two declared @Retryable handlers genuinely do carry the retry advice,
        // so the assertion above is rejecting a real distinction rather than always being false
        assertThat(hasRetryAdvice(increaseNumberOfStudentsCommandHandler)).isTrue();
        assertThat(hasRetryAdvice(updateCourseRatingCommandHandler)).isTrue();
    }

    @Test
    void productionContext_siblingHandler_optimisticLockFailureOnSave_isNotRetried() {
        // given - the sibling's save clashes on the version under the real production wiring
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1));

        // when
        final ApproveCourseCommand command = new ApproveCourseCommand(uuid);

        // then - without retry advice the optimistic-lock failure surfaces on the first attempt; the
        // handler loads and saves exactly once rather than burning the retry budget the targets get
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> approveCourseCommandHandler.handle(command));
        verify(repository, times(1)).findByUuid(uuid);
        verify(repository, times(1)).save(any(Course.class));
    }

    @Test
    void productionContext_siblingHandler_nonRetryableFailure_isNotRetried() {
        // given - the sibling's save fails with an unrelated exception
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class))).thenThrow(new IllegalStateException("boom"));

        // when
        final ApproveCourseCommand command = new ApproveCourseCommand(uuid);

        // then - the sibling has no retry advice at all, so any failure propagates after a single save
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> approveCourseCommandHandler.handle(command));
        verify(repository, times(1)).findByUuid(uuid);
        verify(repository, times(1)).save(any(Course.class));
    }

    private static boolean hasRetryAdvice(Object bean) {
        return adviceClassNames(bean).anyMatch(name -> name.contains("Retry"));
    }

    private static boolean hasTransactionAdvice(Object bean) {
        if (!(bean instanceof Advised advised)) {
            return false;
        }
        for (Advisor advisor : advised.getAdvisors()) {
            if (advisor.getAdvice() instanceof TransactionInterceptor) {
                return true;
            }
        }
        return false;
    }

    private static java.util.stream.Stream<String> adviceClassNames(Object bean) {
        if (!(bean instanceof Advised advised)) {
            return java.util.stream.Stream.empty();
        }
        return java.util.Arrays.stream(advised.getAdvisors())
                .map(Advisor::getAdvice)
                .map(Advice::getClass)
                .map(Class::getName);
    }

    private static Course newCourse() {
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        return new Course(createCourseCommand, 15);
    }
}
