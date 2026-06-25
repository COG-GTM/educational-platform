package com.educational.platform.common.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Verifies the real JPA persistence contract of {@link FailedIntegrationEventRecord} through
 * {@link FailedIntegrationEventRepository}. Complements the reflection-based schema/annotation
 * tests by exercising the entity against an actual database (ID generation, full field
 * round-trip, enum stored via {@code EnumType.STRING}, the {@code resolve()} mutation, and the
 * {@code nullable = false} column constraints).
 */
@DataJpaTest
class FailedIntegrationEventRecordPersistenceTest {

    @Autowired
    private FailedIntegrationEventRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void save_assignsGeneratedIdentity() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.SomeEvent", "SomeEvent[id=1]", "boom", 3);
        assertThat(record.getId()).isNull();

        // when
        final FailedIntegrationEventRecord saved = repository.save(record);

        // then
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void save_thenFindById_roundTripsAllFields() {
        // given
        final Instant before = Instant.now();
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.SomeEvent", "SomeEvent[id=42]", "DB connection lost", 3);
        final Long id = repository.save(record).getId();
        flushAndClear();

        // when
        final FailedIntegrationEventRecord reloaded = repository.findById(id).orElseThrow();

        // then
        assertThat(reloaded.getEventClassName()).isEqualTo("com.example.SomeEvent");
        assertThat(reloaded.getEventPayload()).isEqualTo("SomeEvent[id=42]");
        assertThat(reloaded.getExceptionMessage()).isEqualTo("DB connection lost");
        assertThat(reloaded.getRetryCount()).isEqualTo(3);
        assertThat(reloaded.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
        assertThat(reloaded.getTimestamp())
                .isNotNull()
                .isCloseTo(before, within(1, ChronoUnit.MINUTES));
    }

    @Test
    void save_defaultsStatusToFailed() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.SomeEvent", "SomeEvent[id=1]", "boom", 3);

        // when
        final Long id = repository.save(record).getId();
        flushAndClear();

        // then
        assertThat(repository.findById(id).orElseThrow().getStatus())
                .isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void resolve_thenSave_persistsResolvedStatus() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.SomeEvent", "SomeEvent[id=1]", "boom", 3);
        final Long id = repository.save(record).getId();

        // when
        record.resolve();
        repository.save(record);
        flushAndClear();

        // then
        assertThat(repository.findById(id).orElseThrow().getStatus())
                .isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.RESOLVED);
    }

    @Test
    void findAll_returnsAllSavedRecords() {
        // given
        repository.save(new FailedIntegrationEventRecord("com.example.A", "A[]", "e1", 3));
        repository.save(new FailedIntegrationEventRecord("com.example.B", "B[]", "e2", 3));
        flushAndClear();

        // then
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void deleteById_removesRecord() {
        // given
        final Long id = repository.save(
                new FailedIntegrationEventRecord("com.example.A", "A[]", "e1", 3)).getId();
        flushAndClear();

        // when
        repository.deleteById(id);
        flushAndClear();

        // then
        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void save_withNullEventPayload_violatesNotNullConstraint() {
        // given - event_payload column is declared nullable = false
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.SomeEvent", null, "boom", 3);

        // when / then
        assertThatThrownBy(() -> repository.saveAndFlush(record))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_withNullExceptionMessage_violatesNotNullConstraint() {
        // given - exception_message column is declared nullable = false
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.SomeEvent", "SomeEvent[id=1]", null, 3);

        // when / then
        assertThatThrownBy(() -> repository.saveAndFlush(record))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
