package com.educational.platform.courses.bridge;

import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Re-publishes {@code SendCourseToApproveIntegrationEvent} messages produced by {@code courses-py} as in-JVM Spring
 * events, so the unchanged {@code administration} module ({@code SendCourseToApproveIntegrationEventHandler})
 * keeps creating course proposals regardless of which implementation of {@code courses} sent the course for approval.
 */
public class InboundIntegrationEventBridge {

    private static final Logger log = LoggerFactory.getLogger(InboundIntegrationEventBridge.class);

    private final ApplicationEventPublisher eventPublisher;

    public InboundIntegrationEventBridge(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @RabbitListener(queues = IntegrationEventTopics.MONOLITH_SEND_COURSE_TO_APPROVE_QUEUE)
    public void onSendCourseToApprove(String json) {
        IntegrationEventJson.readCourseId(json).ifPresentOrElse(
                courseId -> eventPublisher.publishEvent(new SendCourseToApproveIntegrationEvent(courseId)),
                () -> log.warn("Ignoring {} message without courseId: {}", IntegrationEventTopics.SEND_COURSE_TO_APPROVE, json));
    }
}
