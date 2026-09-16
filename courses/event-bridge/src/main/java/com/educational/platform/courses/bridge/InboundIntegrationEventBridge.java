package com.educational.platform.courses.bridge;

import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Re-publishes {@code SendCourseToApproveIntegrationEvent} messages produced by {@code courses-py} as in-JVM Spring
 * events, so the unchanged {@code administration} module ({@code SendCourseToApproveIntegrationEventHandler})
 * keeps creating course proposals regardless of which implementation of {@code courses} sent the course for approval.
 */
public class InboundIntegrationEventBridge {

    private final ApplicationEventPublisher eventPublisher;

    public InboundIntegrationEventBridge(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @RabbitListener(queues = IntegrationEventTopics.MONOLITH_SEND_COURSE_TO_APPROVE_QUEUE)
    public void onSendCourseToApprove(String json) {
        var courseId = IntegrationEventJson.readCourseId(json).orElseThrow(() -> new AmqpRejectAndDontRequeueException(
                IntegrationEventTopics.SEND_COURSE_TO_APPROVE + " message without courseId: " + json));
        eventPublisher.publishEvent(new SendCourseToApproveIntegrationEvent(courseId));
    }
}
