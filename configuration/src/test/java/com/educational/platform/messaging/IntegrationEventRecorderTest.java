package com.educational.platform.messaging;

import java.util.UUID;

import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link IntegrationEventRecorder}: only events declared in an
 * {@code integration.event} package are recorded, and the recorder exposes them
 * via {@link IntegrationEventRecorder#events()} and
 * {@link IntegrationEventRecorder#eventsOfType(Class)}.
 */
public class IntegrationEventRecorderTest {

	private final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

	private IntegrationEventRecorder sut;

	@BeforeEach
	void setUp() {
		sut = new IntegrationEventRecorder();
	}

	@Test
	void record_integrationEvent_recorded() {
		// given
		final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(courseId);

		// when
		sut.record(event);

		// then
		assertThat(sut.events()).containsExactly(event);
	}

	@Test
	void record_nonIntegrationEvent_ignored() {
		// when
		sut.record("plain application event");
		sut.record(new Object());

		// then
		assertThat(sut.events()).isEmpty();
	}

	@Test
	void eventsOfType_mixedEvents_returnsOnlyMatchingTypeInPublicationOrder() {
		// given
		final SendCourseToApproveIntegrationEvent first = new SendCourseToApproveIntegrationEvent(courseId);
		final CourseRatingRecalculatedIntegrationEvent rating =
				new CourseRatingRecalculatedIntegrationEvent(courseId, 4.5);
		final SendCourseToApproveIntegrationEvent second =
				new SendCourseToApproveIntegrationEvent(UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

		// when
		sut.record(first);
		sut.record(rating);
		sut.record(second);

		// then
		assertThat(sut.eventsOfType(SendCourseToApproveIntegrationEvent.class)).containsExactly(first, second);
		assertThat(sut.eventsOfType(CourseRatingRecalculatedIntegrationEvent.class)).containsExactly(rating);
	}

	@Test
	void eventsOfType_noMatchingEvents_returnsEmptyList() {
		// given
		sut.record(new SendCourseToApproveIntegrationEvent(courseId));

		// then
		assertThat(sut.eventsOfType(CourseRatingRecalculatedIntegrationEvent.class)).isEmpty();
	}

	@Test
	void reset_recordedEvents_cleared() {
		// given
		sut.record(new SendCourseToApproveIntegrationEvent(courseId));

		// when
		sut.reset();

		// then
		assertThat(sut.events()).isEmpty();
	}

	@Test
	void events_returnedList_isSnapshotAndUnmodifiable() {
		// given
		final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(courseId);
		sut.record(event);
		final var snapshot = sut.events();

		// when
		sut.record(new SendCourseToApproveIntegrationEvent(UUID.randomUUID()));

		// then
		assertThat(snapshot).containsExactly(event);
		assertThatThrownBy(() -> snapshot.add(event)).isInstanceOf(UnsupportedOperationException.class);
	}
}
