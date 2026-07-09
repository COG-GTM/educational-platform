package com.educational.platform.common.outbox;

import java.lang.reflect.Method;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IntegrationEventOutboxTest {

    @Mock
    private IntegrationEventOutboxRepository repository;

    @Test
    void publish_serializableEvent_entrySavedAsNew() throws Exception {
        final IntegrationEventOutbox sut = new IntegrationEventOutbox(repository);
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        sut.publish(new SampleIntegrationEvent(uuid));

        final ArgumentCaptor<IntegrationEventOutboxEntry> argument = ArgumentCaptor.forClass(IntegrationEventOutboxEntry.class);
        verify(repository).save(argument.capture());
        final IntegrationEventOutboxEntry entry = argument.getValue();
        assertThat(entry.getStatus()).isEqualTo(OutboxEntryStatus.NEW);
        assertThat(entry.getEventType()).isEqualTo(SampleIntegrationEvent.class.getName());
        assertThat(new ObjectMapper().readValue(entry.getPayload(), SampleIntegrationEvent.class))
                .isEqualTo(new SampleIntegrationEvent(uuid));
    }

    @Test
    void publish_nonSerializableEvent_illegalArgumentException() {
        final IntegrationEventOutbox sut = new IntegrationEventOutbox(repository);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> sut.publish(new Object()));
    }

    @Test
    void publish_isTransactionalWithMandatoryPropagation() throws Exception {
        // then
        final Method method = IntegrationEventOutbox.class.getMethod("publish", Object.class);
        final Transactional transactional = method.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.MANDATORY);
    }
}
