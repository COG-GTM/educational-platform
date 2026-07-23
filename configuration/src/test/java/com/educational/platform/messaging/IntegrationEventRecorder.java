package com.educational.platform.messaging;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.event.EventListener;

/**
 * Records integration events published to the application event bus, so tests can assert on
 * the messages exchanged between bounded contexts. An event is considered an integration event
 * if it is declared in an {@code integration.event} package of one of the modules.
 */
public class IntegrationEventRecorder {

	private final List<Object> events = new CopyOnWriteArrayList<>();

	@EventListener
	public void record(Object event) {
		if (isIntegrationEvent(event)) {
			events.add(event);
		}
	}

	public List<Object> events() {
		return List.copyOf(events);
	}

	public <T> List<T> eventsOfType(Class<T> type) {
		return events.stream()
				.filter(type::isInstance)
				.map(type::cast)
				.toList();
	}

	public void reset() {
		events.clear();
	}

	private static boolean isIntegrationEvent(Object event) {
		return event.getClass().getPackageName().contains(".integration.event");
	}
}
