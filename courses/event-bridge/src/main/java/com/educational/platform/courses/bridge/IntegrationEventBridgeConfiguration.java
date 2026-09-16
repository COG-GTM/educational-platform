package com.educational.platform.courses.bridge;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Strangler-fig event bridge between the monolith's in-JVM Spring events and the external message broker used by
 * {@code courses-py}. Disabled unless {@code courses.event-bridge.enabled=true}; the RabbitMQ connection is
 * configured through the standard {@code spring.rabbitmq.*} properties.
 * <p>
 * Remove this sub-project (and its {@code settings.gradle.kts} / {@code configuration} entries) at cutover, once the
 * remaining Java modules publish to / consume from the broker directly. See {@code courses-py/README.md}.
 */
@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "courses.event-bridge.enabled", havingValue = "true")
public class IntegrationEventBridgeConfiguration {

    @Bean
    TopicExchange integrationEventsExchange() {
        return new TopicExchange(IntegrationEventTopics.EXCHANGE, true, false);
    }

    @Bean
    Queue monolithSendCourseToApproveQueue() {
        return new Queue(IntegrationEventTopics.MONOLITH_SEND_COURSE_TO_APPROVE_QUEUE, true);
    }

    @Bean
    Binding monolithSendCourseToApproveBinding(Queue monolithSendCourseToApproveQueue, TopicExchange integrationEventsExchange) {
        return BindingBuilder.bind(monolithSendCourseToApproveQueue)
                .to(integrationEventsExchange)
                .with(IntegrationEventTopics.SEND_COURSE_TO_APPROVE);
    }

    @Bean
    OutboundIntegrationEventBridge outboundIntegrationEventBridge(AmqpTemplate amqpTemplate) {
        return new OutboundIntegrationEventBridge(amqpTemplate);
    }

    @Bean
    InboundIntegrationEventBridge inboundIntegrationEventBridge(ApplicationEventPublisher eventPublisher) {
        return new InboundIntegrationEventBridge(eventPublisher);
    }
}
