package com.educational.platform.courses;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRating;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.NumberOfStudents;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommand;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves the retry feature recovers from an optimistic-lock conflict that surfaces at
 * <em>transaction-commit</em> time - the way the conflict actually arises in production - not only
 * from one thrown synchronously inside the handler method body.
 *
 * <p>Every existing retry test (the mock-based per-handler suites, the {@code @MockitoSpyBean}
 * {@code CourseRetryRecoveryIntegrationTest}, even {@link CourseRetryFreshTransactionPerAttemptTest})
 * forces the conflict by making {@code repository.save(...)} throw <em>inside</em> {@code handle(...)}.
 * But the production handlers call {@code repository.save(course)} under {@code @Transactional}: saving
 * a managed entity defers the SQL flush, so the {@code @Version} check does not run at the {@code save}
 * call - it runs when the transaction flushes on commit. The real
 * {@link ObjectOptimisticLockingFailureException} is therefore raised by the transaction manager's
 * {@code commit(...)} <em>after</em> {@code handle(...)} has already returned, entirely outside the
 * method body. {@link CourseRetryFreshTransactionPerAttemptTest} mocks the transaction manager but
 * still throws at {@code save}, so the conflict is observed while the transaction interceptor is
 * unwinding the body (its rollback branch); nothing yet exercises a failure raised by {@code commit}
 * itself.
 *
 * <p>This is the behaviour the retry-outside-transaction advice ordering exists for: because the
 * {@code @Retryable} advice wraps the {@code @Transactional} advice, a commit-time failure propagates
 * out of the transaction interceptor and back into the retry interceptor, which re-runs the whole
 * load -&gt; mutate -&gt; save -&gt; commit cycle in a brand-new transaction. Were the ordering ever
 * reversed, the commit would happen only after retry had already returned and a commit-time conflict
 * could never be retried.
 *
 * <p>The setup mirrors {@link CourseRetryFreshTransactionPerAttemptTest}: the production
 * {@link CourseRetryConfiguration} ({@code @EnableRetry}) wired with {@code @EnableTransactionManagement}
 * and a mocked {@link PlatformTransactionManager}. Here {@code repository.save(...)} succeeds (the body
 * completes) and {@code commit(...)} is what raises the conflict - exactly the deferred-flush path.
 */
@SpringJUnitConfig(classes = {CourseRetryConfiguration.class, CourseRetryCommitTimeConflictTest.TxConfig.class})
class CourseRetryCommitTimeConflictTest {

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
        // a non-null status is required for the transaction interceptor to take the commit branch;
        // returning a fresh status per call mirrors a fresh transaction per attempt
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> mock(TransactionStatus.class));
    }

    @Test
    void increaseNumberOfStudentsHandler_optimisticLockSurfacingAtCommit_isRetriedAndRecovers() {
        // given - the handler body (load -> increment -> save) completes, and the first commit clashes on
        // the version (deferred flush), while the second commit succeeds
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .doNothing()
                .when(transactionManager).commit(any(TransactionStatus.class));

        // when
        increaseNumberOfStudentsCommandHandler.handle(new IncreaseNumberOfStudentsCommand(uuid));

        // then - the commit-time conflict was retried: a second fresh transaction was begun and committed,
        // the body re-ran (reload + save), and the increment derived from the retry's own fresh reload persisted
        final ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(repository, times(2)).findByUuid(uuid);
        verify(repository, times(2)).save(saved.capture());
        verify(transactionManager, times(2)).getTransaction(any(TransactionDefinition.class));
        verify(transactionManager, times(2)).commit(any(TransactionStatus.class));
        // a commit-time failure never enters the interceptor's rollback branch (the body did not throw)
        verify(transactionManager, never()).rollback(any(TransactionStatus.class));
        assertThat(saved.getValue()).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
    }

    @Test
    void updateCourseRatingHandler_optimisticLockSurfacingAtCommit_isRetriedAndRecovers() {
        // given - the handler body (load -> updateRating -> save) completes, and the first commit clashes on
        // the version (deferred flush), while the second commit succeeds
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .doNothing()
                .when(transactionManager).commit(any(TransactionStatus.class));

        // when
        updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(uuid, 3.2));

        // then - the commit-time conflict was retried: a second fresh transaction was begun and committed,
        // the body re-ran (reload + save), and the command's rating was re-applied on the retry and persisted
        final ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(repository, times(2)).findByUuid(uuid);
        verify(repository, times(2)).save(saved.capture());
        verify(transactionManager, times(2)).getTransaction(any(TransactionDefinition.class));
        verify(transactionManager, times(2)).commit(any(TransactionStatus.class));
        // a commit-time failure never enters the interceptor's rollback branch (the body did not throw)
        verify(transactionManager, never()).rollback(any(TransactionStatus.class));
        assertThat(saved.getValue()).hasFieldOrPropertyWithValue("rating", new CourseRating(3.2));
    }

    @Test
    void increaseNumberOfStudentsHandler_persistentCommitConflict_exhaustsAttemptsThenRethrows() {
        // given - the body always completes but every commit clashes on the version (deferred flush)
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .when(transactionManager).commit(any(TransactionStatus.class));

        // when
        final IncreaseNumberOfStudentsCommand command = new IncreaseNumberOfStudentsCommand(uuid);

        // then - maxAttempts=3 commits were each attempted in their own transaction and the persistent
        // commit-time conflict is rethrown; the body succeeding every time never triggers a rollback
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> increaseNumberOfStudentsCommandHandler.handle(command));
        verify(repository, times(3)).findByUuid(uuid);
        verify(repository, times(3)).save(any(Course.class));
        verify(transactionManager, times(3)).getTransaction(any(TransactionDefinition.class));
        verify(transactionManager, times(3)).commit(any(TransactionStatus.class));
        verify(transactionManager, never()).rollback(any(TransactionStatus.class));
    }

    @Test
    void updateCourseRatingHandler_persistentCommitConflict_exhaustsAttemptsThenRethrows() {
        // given - the body always completes but every commit clashes on the version (deferred flush)
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .when(transactionManager).commit(any(TransactionStatus.class));

        // when
        final UpdateCourseRatingCommand command = new UpdateCourseRatingCommand(uuid, 3.2);

        // then - maxAttempts=3 commits were each attempted in their own transaction and the persistent
        // commit-time conflict is rethrown; the body succeeding every time never triggers a rollback
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> updateCourseRatingCommandHandler.handle(command));
        verify(repository, times(3)).findByUuid(uuid);
        verify(repository, times(3)).save(any(Course.class));
        verify(transactionManager, times(3)).getTransaction(any(TransactionDefinition.class));
        verify(transactionManager, times(3)).commit(any(TransactionStatus.class));
        verify(transactionManager, never()).rollback(any(TransactionStatus.class));
    }

    private static Course newCourse() {
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        return new Course(createCourseCommand, 15);
    }
}
