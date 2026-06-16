package com.educational.platform.courses;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommand;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the <em>runtime</em> consequence of the retry feature's advice ordering: every retry attempt
 * on a {@code @Retryable} course command handler runs in its own fresh transaction (load -&gt;
 * mutate -&gt; save), so a re-attempt can observe a concurrently committed version and recover.
 *
 * <p>{@link CourseRetryTransactionAdviceOrderingTest} proves this <em>statically</em> - it inspects
 * the advisor chain and asserts the {@code @Retryable} advice sits outside the {@code @Transactional}
 * advice. But static ordering alone never exercises the handlers, so nothing yet observes the
 * behaviour that ordering exists for: that each attempt actually <em>begins</em> and completes a
 * separate transaction. Here we wire the production {@link CourseRetryConfiguration}
 * ({@code @EnableRetry}) together with {@code @EnableTransactionManagement} and a mocked
 * {@link PlatformTransactionManager}, drive the handlers through conflict/recovery sequences, then
 * verify the transaction manager interactions per attempt:
 * <ul>
 *   <li>a persistent conflict begins (and rolls back) one transaction per attempt - never reusing a
 *       single shared transaction across the {@code maxAttempts=3} run, and never committing;</li>
 *   <li>a single conflict followed by a successful retry begins two transactions, rolls back the
 *       failed attempt's transaction and commits the recovering attempt's - the fresh-transaction
 *       boundary that lets the reload see the conflicting commit and persist.</li>
 * </ul>
 * If the retry advice were ever reordered inside the transaction advice, all attempts would share a
 * single transaction: {@code getTransaction} would be called once (not once per attempt) and this
 * test would fail, where the static ordering test could still be made to pass by a partial change.
 */
@SpringJUnitConfig(classes = {CourseRetryConfiguration.class, CourseRetryFreshTransactionPerAttemptTest.TxConfig.class})
class CourseRetryFreshTransactionPerAttemptTest {

    @Configuration
    @EnableTransactionManagement
    static class TxConfig {

        @Bean
        PlatformTransactionManager transactionManager() {
            return mock(PlatformTransactionManager.class);
        }

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
    private PlatformTransactionManager transactionManager;

    @Autowired
    private CourseRepository repository;

    @Autowired
    private IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    @Autowired
    private UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    @BeforeEach
    void resetMocks() {
        reset(repository, transactionManager);
        // a non-null status is required for the transaction interceptor to take the commit/rollback
        // branches it otherwise skips; returning a fresh status mirrors a fresh transaction per attempt
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> mock(TransactionStatus.class));
    }

    @Test
    void increaseNumberOfStudentsHandler_persistentConflict_beginsAndRollsBackAFreshTransactionPerAttempt() {
        // given - every save clashes on the version, so the full maxAttempts=3 sequence runs
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1));

        // when
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> increaseNumberOfStudentsCommandHandler.handle(new IncreaseNumberOfStudentsCommand(uuid)));

        // then - each of the three attempts opened its own transaction and rolled it back; the
        // exhausted run never committed
        verify(transactionManager, times(3)).getTransaction(any(TransactionDefinition.class));
        verify(transactionManager, times(3)).rollback(any(TransactionStatus.class));
        verify(transactionManager, never()).commit(any(TransactionStatus.class));
    }

    @Test
    void updateCourseRatingHandler_persistentConflict_beginsAndRollsBackAFreshTransactionPerAttempt() {
        // given - every save clashes on the version, so the full maxAttempts=3 sequence runs
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1));

        // when
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(uuid, 3.2)));

        // then - each of the three attempts opened its own transaction and rolled it back; the
        // exhausted run never committed
        verify(transactionManager, times(3)).getTransaction(any(TransactionDefinition.class));
        verify(transactionManager, times(3)).rollback(any(TransactionStatus.class));
        verify(transactionManager, never()).commit(any(TransactionStatus.class));
    }

    @Test
    void increaseNumberOfStudentsHandler_singleConflictThenSuccess_rollsBackFailedAttemptAndCommitsTheRecovery() {
        // given - the first save clashes on the version, the retry persists
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        increaseNumberOfStudentsCommandHandler.handle(new IncreaseNumberOfStudentsCommand(uuid));

        // then - two separate transactions were begun (one per attempt); the failed attempt rolled
        // back and the recovering attempt committed - the fresh-transaction boundary recovery needs
        verify(transactionManager, times(2)).getTransaction(any(TransactionDefinition.class));
        verify(transactionManager, times(1)).rollback(any(TransactionStatus.class));
        verify(transactionManager, times(1)).commit(any(TransactionStatus.class));
    }

    @Test
    void updateCourseRatingHandler_singleConflictThenSuccess_rollsBackFailedAttemptAndCommitsTheRecovery() {
        // given - the first save clashes on the version, the retry persists
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(uuid, 3.2));

        // then - two separate transactions were begun (one per attempt); the failed attempt rolled
        // back and the recovering attempt committed - the fresh-transaction boundary recovery needs
        verify(transactionManager, times(2)).getTransaction(any(TransactionDefinition.class));
        verify(transactionManager, times(1)).rollback(any(TransactionStatus.class));
        verify(transactionManager, times(1)).commit(any(TransactionStatus.class));
    }

    private static Course newCourse() {
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        return new Course(createCourseCommand, 15);
    }
}
