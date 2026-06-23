package com.educational.platform.config.persistence;

import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying that {@link FailedIntegrationEventRecord} can be persisted
 * and retrieved via {@link FailedIntegrationEventRepository} against the actual JPA/H2 schema.
 * Catches entity-mapping or Liquibase-migration mismatches early.
 */
@DataJpaTest
class FailedIntegrationEventRepositoryIntegrationTest {

    @Autowired
    private FailedIntegrationEventRepository repository;

    @Test
    void save_persistsAllFieldsCorrectly() throws Exception {
        // given
        Instant before = Instant.now();
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent",
                "SendCourseToApproveIntegrationEvent[courseId=123e4567-e89b-12d3-a456-426655440001]",
                "DB connection lost",
                "org.springframework.dao.DataAccessResourceFailureException",
                3
        );

        // when
        FailedIntegrationEventRecord saved = repository.save(record);

        // then
        assertThat(getField(saved, "id")).isNotNull();
        assertThat(getField(saved, "eventClassName"))
                .isEqualTo("com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent");
        assertThat(getField(saved, "eventPayload"))
                .isEqualTo("SendCourseToApproveIntegrationEvent[courseId=123e4567-e89b-12d3-a456-426655440001]");
        assertThat(getField(saved, "exceptionMessage")).isEqualTo("DB connection lost");
        assertThat(getField(saved, "exceptionClassName"))
                .isEqualTo("org.springframework.dao.DataAccessResourceFailureException");
        assertThat((int) getField(saved, "retryCount")).isEqualTo(3);
        assertThat((FailedIntegrationEventRecord.Status) getField(saved, "status"))
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
        Instant createdAt = (Instant) getField(saved, "createdAt");
        assertThat(createdAt).isAfterOrEqualTo(before);
    }

    @Test
    void findById_afterSave_returnsPersistedRecord() throws Exception {
        // given
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.TestEvent", "test-payload", "test error",
                "java.lang.RuntimeException", 3
        );
        FailedIntegrationEventRecord saved = repository.save(record);
        Long id = (Long) getField(saved, "id");

        // when
        Optional<FailedIntegrationEventRecord> found = repository.findById(id);

        // then
        assertThat(found).isPresent();
        assertThat(getField(found.get(), "eventClassName")).isEqualTo("com.example.TestEvent");
        assertThat(getField(found.get(), "eventPayload")).isEqualTo("test-payload");
    }

    @Test
    void save_multipleRecords_allRetrievable() {
        // given
        FailedIntegrationEventRecord record1 = new FailedIntegrationEventRecord(
                "com.example.EventA", "payload-a", "error-a", "java.lang.RuntimeException", 3);
        FailedIntegrationEventRecord record2 = new FailedIntegrationEventRecord(
                "com.example.EventB", "payload-b", "error-b", "java.lang.IllegalStateException", 5);

        // when
        repository.save(record1);
        repository.save(record2);

        // then
        List<FailedIntegrationEventRecord> all = repository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    void save_withNullExceptionMessage_persistsSuccessfully() throws Exception {
        // given
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.TestEvent", "payload", null, "java.lang.RuntimeException", 3);

        // when
        FailedIntegrationEventRecord saved = repository.save(record);

        // then
        Long id = (Long) getField(saved, "id");
        Optional<FailedIntegrationEventRecord> found = repository.findById(id);
        assertThat(found).isPresent();
        assertThat(getField(found.get(), "exceptionMessage")).isNull();
    }

    @Test
    void save_withNullExceptionClassName_persistsSuccessfully() throws Exception {
        // given
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.TestEvent", "payload", "error msg", null, 3);

        // when
        FailedIntegrationEventRecord saved = repository.save(record);

        // then
        Long id = (Long) getField(saved, "id");
        Optional<FailedIntegrationEventRecord> found = repository.findById(id);
        assertThat(found).isPresent();
        assertThat(getField(found.get(), "exceptionClassName")).isNull();
    }

    @Test
    void save_statusPersistedAsString() throws Exception {
        // given
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.TestEvent", "payload", "error", "java.lang.RuntimeException", 3);

        // when
        FailedIntegrationEventRecord saved = repository.save(record);

        // then
        Long id = (Long) getField(saved, "id");
        Optional<FailedIntegrationEventRecord> found = repository.findById(id);
        assertThat(found).isPresent();
        assertThat((FailedIntegrationEventRecord.Status) getField(found.get(), "status"))
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
    }

    @Test
    void save_largePayloadNearColumnLimit_persistsSuccessfully() throws Exception {
        // given
        String largePayload = "x".repeat(4000);
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.TestEvent", largePayload, "error", "java.lang.RuntimeException", 3);

        // when
        FailedIntegrationEventRecord saved = repository.save(record);

        // then
        Long id = (Long) getField(saved, "id");
        Optional<FailedIntegrationEventRecord> found = repository.findById(id);
        assertThat(found).isPresent();
        assertThat(getField(found.get(), "eventPayload")).isEqualTo(largePayload);
    }

    @Test
    void save_generatesSequentialIds() throws Exception {
        // given
        FailedIntegrationEventRecord record1 = new FailedIntegrationEventRecord(
                "com.example.EventA", "payload-a", "error", "java.lang.RuntimeException", 1);
        FailedIntegrationEventRecord record2 = new FailedIntegrationEventRecord(
                "com.example.EventB", "payload-b", "error", "java.lang.RuntimeException", 2);

        // when
        FailedIntegrationEventRecord saved1 = repository.save(record1);
        FailedIntegrationEventRecord saved2 = repository.save(record2);

        // then
        Long id1 = (Long) getField(saved1, "id");
        Long id2 = (Long) getField(saved2, "id");
        assertThat(id1).isNotNull();
        assertThat(id2).isNotNull();
        assertThat(id2).isGreaterThan(id1);
    }

    @Test
    void count_afterSavingRecords_returnsCorrectCount() {
        // given
        repository.save(new FailedIntegrationEventRecord(
                "com.example.EventA", "payload-a", "error", "java.lang.RuntimeException", 3));
        repository.save(new FailedIntegrationEventRecord(
                "com.example.EventB", "payload-b", "error", "java.lang.RuntimeException", 3));

        // then
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void save_withUnicodePayload_persistsAndRetrievesCorrectly() throws Exception {
        // given — unicode characters in event payload (e.g., course names in other languages)
        String unicodePayload = "CourseEvent[name=数学课程, teacher=田中太郎, desc=Ünïcödé]";
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.UnicodeEvent", unicodePayload, "error with émojis 🎉", "java.lang.RuntimeException", 3);

        // when
        FailedIntegrationEventRecord saved = repository.save(record);

        // then
        Long id = (Long) getField(saved, "id");
        Optional<FailedIntegrationEventRecord> found = repository.findById(id);
        assertThat(found).isPresent();
        assertThat(getField(found.get(), "eventPayload")).isEqualTo(unicodePayload);
        assertThat(getField(found.get(), "exceptionMessage")).isEqualTo("error with émojis 🎉");
    }

    @Test
    void save_createdAtTimestampIsChronological() throws Exception {
        // given
        FailedIntegrationEventRecord record1 = new FailedIntegrationEventRecord(
                "com.example.Event1", "payload-1", "error", "java.lang.RuntimeException", 3);
        FailedIntegrationEventRecord saved1 = repository.save(record1);
        Instant createdAt1 = (Instant) getField(saved1, "createdAt");

        // when — slight delay to ensure different timestamps
        Thread.sleep(10);
        FailedIntegrationEventRecord record2 = new FailedIntegrationEventRecord(
                "com.example.Event2", "payload-2", "error", "java.lang.RuntimeException", 3);
        FailedIntegrationEventRecord saved2 = repository.save(record2);
        Instant createdAt2 = (Instant) getField(saved2, "createdAt");

        // then
        assertThat(createdAt2).isAfterOrEqualTo(createdAt1);
    }

    @Test
    void save_withZeroRetryCount_persistsSuccessfully() throws Exception {
        // given — edge case: 0 retries (immediate failure)
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.TestEvent", "payload", "error", "java.lang.RuntimeException", 0);

        // when
        FailedIntegrationEventRecord saved = repository.save(record);

        // then
        Long id = (Long) getField(saved, "id");
        Optional<FailedIntegrationEventRecord> found = repository.findById(id);
        assertThat(found).isPresent();
        assertThat((int) getField(found.get(), "retryCount")).isZero();
    }

    @Test
    void save_withBothNullableFieldsNull_persistsSuccessfully() throws Exception {
        // given — both exceptionMessage and exceptionClassName are nullable
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.TestEvent", "payload", null, null, 3);

        // when
        FailedIntegrationEventRecord saved = repository.save(record);

        // then
        Long id = (Long) getField(saved, "id");
        Optional<FailedIntegrationEventRecord> found = repository.findById(id);
        assertThat(found).isPresent();
        assertThat(getField(found.get(), "exceptionMessage")).isNull();
        assertThat(getField(found.get(), "exceptionClassName")).isNull();
        assertThat(getField(found.get(), "eventClassName")).isEqualTo("com.example.TestEvent");
        assertThat(getField(found.get(), "eventPayload")).isEqualTo("payload");
    }

    @Test
    void save_longExceptionMessage_nearColumnLimit_persistsSuccessfully() throws Exception {
        // given — exceptionMessage column length is 2000
        String longMessage = "x".repeat(2000);
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.TestEvent", "payload", longMessage, "java.lang.RuntimeException", 3);

        // when
        FailedIntegrationEventRecord saved = repository.save(record);

        // then
        Long id = (Long) getField(saved, "id");
        Optional<FailedIntegrationEventRecord> found = repository.findById(id);
        assertThat(found).isPresent();
        assertThat(getField(found.get(), "exceptionMessage")).isEqualTo(longMessage);
    }

    @Test
    void save_withEmptyStrings_persistsAndRetrievesCorrectly() throws Exception {
        // given
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "", "", "", "", 0);

        // when
        FailedIntegrationEventRecord saved = repository.save(record);
        Long id = (Long) getField(saved, "id");
        Optional<FailedIntegrationEventRecord> found = repository.findById(id);

        // then
        assertThat(found).isPresent();
        assertThat(getField(found.get(), "eventClassName")).isEqualTo("");
        assertThat(getField(found.get(), "eventPayload")).isEqualTo("");
        assertThat(getField(found.get(), "exceptionMessage")).isEqualTo("");
        assertThat(getField(found.get(), "exceptionClassName")).isEqualTo("");
        assertThat((int) getField(found.get(), "retryCount")).isZero();
    }

    @Test
    void delete_afterSave_removesRecord() throws Exception {
        // given
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.TestEvent", "payload", "error", "java.lang.RuntimeException", 3);
        FailedIntegrationEventRecord saved = repository.save(record);
        Long id = (Long) getField(saved, "id");

        // when
        repository.deleteById(id);

        // then
        assertThat(repository.findById(id)).isEmpty();
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
