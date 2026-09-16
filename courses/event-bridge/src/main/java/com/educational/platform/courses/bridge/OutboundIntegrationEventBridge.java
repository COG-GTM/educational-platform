package com.educational.platform.courses.bridge;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.nio.charset.StandardCharsets;

/**
 * Forwards the in-JVM Spring integration events that the {@code courses} bounded context consumes to the message
 * broker, so that the Python {@code courses-py} service receives them while both implementations coexist.
 * <p>
 * Only the four events handled by {@code courses} are bridged. {@code SendCourseToApproveIntegrationEvent} is
 * bridged in the opposite direction by {@link InboundIntegrationEventBridge}.
 * <p>
 * Listeners are {@link Async} like the in-JVM {@code *IntegrationEventHandler}s they stand in for: a broker outage is
 * logged by the async executor and does not fail the request that raised the event (at-least-once delivery is not
 * guaranteed; see the cutover checklist in {@code courses-py/README.md} for the outbox follow-up).
 */
public class OutboundIntegrationEventBridge {

    private final AmqpTemplate amqpTemplate;

    public OutboundIntegrationEventBridge(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    @Async
    @EventListener
    public void on(CourseApprovedByAdminIntegrationEvent event) {
        send(IntegrationEventTopics.COURSE_APPROVED_BY_ADMIN, IntegrationEventJson.courseId(event.courseId()));
    }

    @Async
    @EventListener
    public void on(StudentEnrolledToCourseIntegrationEvent event) {
        send(IntegrationEventTopics.STUDENT_ENROLLED_TO_COURSE,
                IntegrationEventJson.courseIdAndUsername(event.courseId(), event.username()));
    }

    @Async
    @EventListener
    public void on(UserCreatedIntegrationEvent event) {
        send(IntegrationEventTopics.USER_CREATED, IntegrationEventJson.usernameAndEmail(event.username(), event.email()));
    }

    @Async
    @EventListener
    public void on(CourseRatingRecalculatedIntegrationEvent event) {
        send(IntegrationEventTopics.COURSE_RATING_RECALCULATED,
                IntegrationEventJson.courseIdAndRating(event.courseId(), event.rating()));
    }

    private void send(String routingKey, String json) {
        var message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        amqpTemplate.send(IntegrationEventTopics.EXCHANGE, routingKey, message);
    }
}
