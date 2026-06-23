package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.CreateCourseProposalCommandHandler;
import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.courses.course.approve.ApproveCourseCommandHandler;
import com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;
import com.educational.platform.courses.teacher.create.CreateTeacherCommandHandler;
import com.educational.platform.courses.teacher.create.UserCreatedIntegrationEventHandler;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests concurrent invocations of {@code recover()} across all integration event handlers.
 * <p>
 * Handler fields ({@code commandHandler}, {@code failedIntegrationEventRepository}) are
 * {@code private final}, so handlers are inherently thread-safe for concurrent recovery
 * calls. These tests document and guard that contract — if someone adds mutable state,
 * these tests may begin to fail intermittently.
 * <p>
 * Complements the existing {@code concurrentInvocations_areIndependent} tests which cover
 * the {@code handleEvent()} path but not the {@code recover()} path.
 */
class IntegrationEventHandlerConcurrentRecoverTest {

    private static final int THREAD_COUNT = 8;

    @Test
    void sendCourseToApproveHandler_concurrentRecover_allRecordsSaved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);

        runConcurrentRecover(THREAD_COUNT, i -> {
            var event = new SendCourseToApproveIntegrationEvent(
                    UUID.fromString("123e4567-e89b-12d3-a456-42665544" + String.format("%04d", i)));
            handler.recover(new DataAccessResourceFailureException("error-" + i), event);
        });

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(THREAD_COUNT)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(THREAD_COUNT);
        assertAllRecordsHaveDistinctPayloads(captor);
    }

    @Test
    void courseApprovedHandler_concurrentRecover_allRecordsSaved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);

        runConcurrentRecover(THREAD_COUNT, i -> {
            var event = new CourseApprovedByAdminIntegrationEvent(
                    UUID.fromString("123e4567-e89b-12d3-a456-42665544" + String.format("%04d", i)));
            handler.recover(new DataAccessResourceFailureException("error-" + i), event);
        });

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(THREAD_COUNT)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(THREAD_COUNT);
        assertAllRecordsHaveDistinctPayloads(captor);
    }

    @Test
    void studentEnrolledHandler_concurrentRecover_allRecordsSaved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);

        runConcurrentRecover(THREAD_COUNT, i -> {
            var event = new StudentEnrolledToCourseIntegrationEvent(
                    UUID.fromString("123e4567-e89b-12d3-a456-42665544" + String.format("%04d", i)),
                    "student-" + i);
            handler.recover(new DataAccessResourceFailureException("error-" + i), event);
        });

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(THREAD_COUNT)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(THREAD_COUNT);
        assertAllRecordsHaveDistinctPayloads(captor);
    }

    @Test
    void courseRatingHandler_concurrentRecover_allRecordsSaved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);

        runConcurrentRecover(THREAD_COUNT, i -> {
            var event = new CourseRatingRecalculatedIntegrationEvent(
                    UUID.fromString("123e4567-e89b-12d3-a456-42665544" + String.format("%04d", i)),
                    1.0 + i * 0.5);
            handler.recover(new DataAccessResourceFailureException("error-" + i), event);
        });

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(THREAD_COUNT)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(THREAD_COUNT);
        assertAllRecordsHaveDistinctPayloads(captor);
    }

    @Test
    void userCreatedHandler_concurrentRecover_allRecordsSaved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);

        runConcurrentRecover(THREAD_COUNT, i -> {
            var event = new UserCreatedIntegrationEvent("teacher-" + i, "teacher" + i + "@test.com");
            handler.recover(new DataAccessResourceFailureException("error-" + i), event);
        });

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(THREAD_COUNT)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(THREAD_COUNT);
        assertAllRecordsHaveDistinctPayloads(captor);
    }

    // --- Helpers ---

    @FunctionalInterface
    private interface IndexedAction {
        void execute(int index) throws Exception;
    }

    private void runConcurrentRecover(int threadCount, IndexedAction action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    action.execute(index);
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            });
        }

        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS))
                .as("All threads should complete within timeout")
                .isTrue();
        assertThat(errorCount.get())
                .as("No thread should have thrown an exception")
                .isZero();
    }

    private void assertAllRecordsHaveDistinctPayloads(ArgumentCaptor<FailedIntegrationEventRecord> captor) throws Exception {
        Field payloadField = FailedIntegrationEventRecord.class.getDeclaredField("eventPayload");
        payloadField.setAccessible(true);

        long distinctPayloads = captor.getAllValues().stream()
                .map(record -> {
                    try {
                        return (String) payloadField.get(record);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .distinct()
                .count();

        assertThat(distinctPayloads)
                .as("Each concurrent recover() call should produce a record with distinct payload")
                .isEqualTo(captor.getAllValues().size());
    }
}
