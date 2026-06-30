package com.educational.platform.common.event;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FailedIntegrationEventRepositoryTest {

    @Autowired
    private FailedIntegrationEventRepository sut;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void save_generatesIdentityAndPersistsAllColumns() {
        // given
        final FailedIntegrationEvent event = FailedIntegrationEvent.of(
                "com.educational.platform.SomeEvent", "{\"id\":1}", "boom", 3);

        // when
        final FailedIntegrationEvent saved = sut.save(event);

        // then
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void findById_returnsRoundTrippedEntityWithAllFieldsPreserved() {
        // given
        final FailedIntegrationEvent saved = sut.save(FailedIntegrationEvent.of(
                "com.educational.platform.SomeEvent", "{\"id\":1}", "boom", 3));
        entityManager.flush();
        entityManager.clear();

        // when
        final FailedIntegrationEvent found = sut.findById(saved.getId()).orElseThrow();

        // then
        assertThat(found.getEventClassName()).isEqualTo("com.educational.platform.SomeEvent");
        assertThat(found.getEventPayload()).isEqualTo("{\"id\":1}");
        assertThat(found.getExceptionMessage()).isEqualTo("boom");
        assertThat(found.getRetryCount()).isEqualTo(3);
        assertThat(found.getStatus()).isEqualTo(FailedEventStatus.FAILED);
        assertThat(found.getTimestamp()).isNotNull();
    }

    @Test
    void save_persistsNullPayloadAndExceptionMessage() {
        // given
        final FailedIntegrationEvent saved = sut.save(
                FailedIntegrationEvent.of("Event", null, null, 0));
        entityManager.flush();
        entityManager.clear();

        // when
        final FailedIntegrationEvent found = sut.findById(saved.getId()).orElseThrow();

        // then
        assertThat(found.getEventPayload()).isNull();
        assertThat(found.getExceptionMessage()).isNull();
        assertThat(found.getRetryCount()).isZero();
        assertThat(found.getStatus()).isEqualTo(FailedEventStatus.FAILED);
    }

    @Test
    void resolve_isPersistedAfterUpdate() {
        // given
        final FailedIntegrationEvent saved = sut.save(
                FailedIntegrationEvent.of("Event", "{}", "msg", 1));

        // when
        saved.resolve();
        sut.save(saved);
        entityManager.flush();
        entityManager.clear();

        // then
        final FailedIntegrationEvent found = sut.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(FailedEventStatus.RESOLVED);
    }

    @Test
    void findAll_returnsAllPersistedDeadLetterEntries() {
        // given
        sut.save(FailedIntegrationEvent.of("EventA", "{}", "a", 1));
        sut.save(FailedIntegrationEvent.of("EventB", "{}", "b", 2));

        // when
        final List<FailedIntegrationEvent> all = sut.findAll();

        // then
        assertThat(all)
                .hasSize(2)
                .extracting(FailedIntegrationEvent::getEventClassName)
                .containsExactlyInAnyOrder("EventA", "EventB");
    }

    @Test
    void delete_removesPersistedEntry() {
        // given
        final FailedIntegrationEvent saved = sut.save(
                FailedIntegrationEvent.of("Event", "{}", "msg", 1));

        // when
        sut.deleteById(saved.getId());
        entityManager.flush();

        // then
        assertThat(sut.findById(saved.getId())).isEmpty();
        assertThat(sut.count()).isZero();
    }
}
