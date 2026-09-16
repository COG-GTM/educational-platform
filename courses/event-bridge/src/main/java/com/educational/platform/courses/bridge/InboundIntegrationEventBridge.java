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

    /** Same limit as the {@code courses-py} consumer (MAX_BODY_BYTES); a valid payload is well under 1 KiB. */
    static final int MAX_BODY_LENGTH = 64 * 1024;

    private final ApplicationEventPublisher eventPublisher;

    public InboundIntegrationEventBridge(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @RabbitListener(queues = IntegrationEventTopics.MONOLITH_SEND_COURSE_TO_APPROVE_QUEUE)
    public void onSendCourseToApprove(String json) {
        if (json.length() > MAX_BODY_LENGTH) {
            throw new AmqpRejectAndDontRequeueException(IntegrationEventTopics.SEND_COURSE_TO_APPROVE
                    + " message of " + json.length() + " chars exceeds " + MAX_BODY_LENGTH);
        }
        var courseId = IntegrationEventJson.readCourseId(json).orElseThrow(() -> new AmqpRejectAndDontRequeueException(
                IntegrationEventTopics.SEND_COURSE_TO_APPROVE + " message of " + json.length()
                        + " chars without a valid courseId"));
        eventPublisher.publishEvent(new SendCourseToApproveIntegrationEvent(courseId));
    }
}
