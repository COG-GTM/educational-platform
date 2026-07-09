package com.educational.platform.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional outbox for integration events.
 *
 * <p>Publishers call {@link #publish(Object)} instead of {@code ApplicationEventPublisher.publishEvent}.
 * The event is serialized and stored in the {@code integration_event_outbox} table within the caller's
 * transaction, so the event is durable exactly when the business change commits — and discarded when it
 * rolls back. The {@link IntegrationEventOutboxRelay} later dispatches stored events to in-process
 * listeners.
 */
@Component
public class IntegrationEventOutbox {

    private final IntegrationEventOutboxRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IntegrationEventOutbox(IntegrationEventOutboxRepository repository) {
        this.repository = repository;
    }

    /**
     * Stores the event in the outbox within the caller's active transaction.
     *
     * @param event integration event to store
     * @throws IllegalArgumentException if the event cannot be serialized
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(Object event) {
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Integration event of type [" + event.getClass().getName() + "] is not serializable", e);
        }
        repository.save(new IntegrationEventOutboxEntry(event.getClass().getName(), payload));
    }
}
