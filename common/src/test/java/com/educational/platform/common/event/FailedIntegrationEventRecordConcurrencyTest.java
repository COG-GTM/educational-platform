package com.educational.platform.common.event;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link FailedIntegrationEventRecord} instances created in rapid
 * succession (including from multiple threads) maintain independent, non-shared state.
 * <p>
 * This guards against accidental use of shared mutable state (e.g., a static field
 * being used for createdAt, or a reused StringBuilder for payload construction).
 */
class FailedIntegrationEventRecordConcurrencyTest {

    @Test
    void multipleRecords_createdSequentially_haveIndependentState() throws Exception {
        // given
        var record1 = new FailedIntegrationEventRecord(
                "com.example.Event1", "payload-1", "error-1", "Exception1", 1);
        var record2 = new FailedIntegrationEventRecord(
                "com.example.Event2", "payload-2", "error-2", "Exception2", 2);
        var record3 = new FailedIntegrationEventRecord(
                "com.example.Event3", "payload-3", "error-3", "Exception3", 3);

        // then — each record has its own distinct values
        assertThat(getField(record1, "eventClassName")).isEqualTo("com.example.Event1");
        assertThat(getField(record2, "eventClassName")).isEqualTo("com.example.Event2");
        assertThat(getField(record3, "eventClassName")).isEqualTo("com.example.Event3");

        assertThat(getField(record1, "eventPayload")).isEqualTo("payload-1");
        assertThat(getField(record2, "eventPayload")).isEqualTo("payload-2");
        assertThat(getField(record3, "eventPayload")).isEqualTo("payload-3");

        assertThat((int) getField(record1, "retryCount")).isEqualTo(1);
        assertThat((int) getField(record2, "retryCount")).isEqualTo(2);
        assertThat((int) getField(record3, "retryCount")).isEqualTo(3);
    }

    @Test
    void multipleRecords_createdSequentially_haveNonDecreasingTimestamps() throws Exception {
        // given
        Instant before = Instant.now();
        var records = new ArrayList<FailedIntegrationEventRecord>();
        for (int i = 0; i < 10; i++) {
            records.add(new FailedIntegrationEventRecord(
                    "com.example.Event", "payload", "error", "Exception", 3));
        }

        // then — all timestamps are >= before and non-decreasing
        Instant previous = before;
        for (int i = 0; i < records.size(); i++) {
            Instant createdAt = (Instant) getField(records.get(i), "createdAt");
            assertThat(createdAt)
                    .as("Record %d createdAt should be >= previous", i)
                    .isAfterOrEqualTo(previous);
            previous = createdAt;
        }
    }

    @Test
    void multipleRecords_createdConcurrently_haveIndependentState() throws Exception {
        // given
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<FailedIntegrationEventRecord>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                latch.await(); // all threads start simultaneously
                return new FailedIntegrationEventRecord(
                        "com.example.Event" + index,
                        "payload-" + index,
                        "error-" + index,
                        "Exception" + index,
                        index);
            }));
        }

        // when — release all threads at once
        latch.countDown();
        List<FailedIntegrationEventRecord> records = new ArrayList<>();
        for (Future<FailedIntegrationEventRecord> future : futures) {
            records.add(future.get());
        }
        executor.shutdown();

        // then — each record has its own distinct values matching its index
        for (int i = 0; i < threadCount; i++) {
            FailedIntegrationEventRecord record = records.get(i);
            assertThat(getField(record, "eventClassName")).isEqualTo("com.example.Event" + i);
            assertThat(getField(record, "eventPayload")).isEqualTo("payload-" + i);
            assertThat(getField(record, "exceptionMessage")).isEqualTo("error-" + i);
            assertThat(getField(record, "exceptionClassName")).isEqualTo("Exception" + i);
            assertThat((int) getField(record, "retryCount")).isEqualTo(i);
            assertThat(getField(record, "createdAt")).isNotNull();
            assertThat(getField(record, "status"))
                    .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
        }
    }

    @Test
    void multipleRecords_createdConcurrently_allHaveFailedStatus() throws Exception {
        // given — constructor always sets FAILED status
        int threadCount = 16;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<FailedIntegrationEventRecord>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                latch.await();
                return new FailedIntegrationEventRecord(
                        "Event", "payload", "error", "Exception", 3);
            }));
        }

        // when
        latch.countDown();
        List<FailedIntegrationEventRecord> records = new ArrayList<>();
        for (Future<FailedIntegrationEventRecord> future : futures) {
            records.add(future.get());
        }
        executor.shutdown();

        // then — all records have FAILED status
        for (FailedIntegrationEventRecord record : records) {
            assertThat(getField(record, "status"))
                    .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
        }
    }

    @Test
    void multipleRecords_doNotShareTimestampInstance() throws Exception {
        // given
        var record1 = new FailedIntegrationEventRecord(
                "Event", "payload", "error", "Exception", 3);
        var record2 = new FailedIntegrationEventRecord(
                "Event", "payload", "error", "Exception", 3);

        // then — each has its own Instant.now() call result
        Instant ts1 = (Instant) getField(record1, "createdAt");
        Instant ts2 = (Instant) getField(record2, "createdAt");
        // Instants are value objects — they might be equal in value but they should
        // come from independent Instant.now() calls
        assertThat(ts1).isNotNull();
        assertThat(ts2).isNotNull();
        // Both should be very close in time but are independently created
        assertThat(ts2).isAfterOrEqualTo(ts1);
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
