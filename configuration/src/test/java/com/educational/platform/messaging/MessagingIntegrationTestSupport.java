package com.educational.platform.messaging;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;

/**
 * Base class for integration tests of the messaging infrastructure between bounded contexts.
 *
 * <p>Boots the full modular monolith so integration events published through
 * {@link ApplicationEventPublisher} are dispatched to the real asynchronous listeners of the
 * other modules. Provides an {@link IntegrationEventRecorder} to assert on published events and
 * {@link #awaitAsyncListeners(ThrowingRunnable)} to wait for {@code @Async} listeners to complete.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IntegrationEventRecorder.class)
public abstract class MessagingIntegrationTestSupport {

	private static final Duration ASYNC_LISTENER_TIMEOUT = Duration.ofSeconds(10);

	@Autowired
	protected ApplicationEventPublisher eventPublisher;

	@Autowired
	protected IntegrationEventRecorder integrationEvents;

	@BeforeEach
	void resetIntegrationEventRecorder() {
		integrationEvents.reset();
	}

	/**
	 * Awaits until the given assertion passes, giving asynchronous event listeners time to process
	 * published integration events.
	 *
	 * @param assertion assertion evaluated repeatedly until it passes or the timeout is reached
	 */
	protected void awaitAsyncListeners(ThrowingRunnable assertion) {
		Awaitility.await()
				.atMost(ASYNC_LISTENER_TIMEOUT)
				.untilAsserted(assertion);
	}
}
