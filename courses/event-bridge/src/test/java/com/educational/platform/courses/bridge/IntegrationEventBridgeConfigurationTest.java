package com.educational.platform.courses.bridge;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListenerAnnotationBeanPostProcessor;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpoint;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntegrationEventBridgeConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(RabbitStubs.class, IntegrationEventBridgeConfiguration.class);

    @Test
    void propertyMissing_bridgeDisabled() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(OutboundIntegrationEventBridge.class);
            assertThat(context).doesNotHaveBean(InboundIntegrationEventBridge.class);
            assertThat(context).doesNotHaveBean(TopicExchange.class);
            assertThat(context).doesNotHaveBean(Queue.class);
            assertThat(context).doesNotHaveBean(Binding.class);
        });
    }

    @Test
    void propertyFalse_bridgeDisabled() {
        runner.withPropertyValues("courses.event-bridge.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(OutboundIntegrationEventBridge.class);
            assertThat(context).doesNotHaveBean(InboundIntegrationEventBridge.class);
        });
    }

    @Test
    void propertyTrue_bridgesAndTopologyRegistered() {
        runner.withPropertyValues("courses.event-bridge.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(OutboundIntegrationEventBridge.class);
            assertThat(context).hasSingleBean(InboundIntegrationEventBridge.class);

            var exchange = context.getBean(TopicExchange.class);
            assertThat(exchange.getName()).isEqualTo("educational-platform.integration-events");
            assertThat(exchange.isDurable()).isTrue();
            assertThat(exchange.isAutoDelete()).isFalse();

            var queue = context.getBean(Queue.class);
            assertThat(queue.getName()).isEqualTo("java-monolith.courses.send-course-to-approve");
            assertThat(queue.isDurable()).isTrue();

            var binding = context.getBean(Binding.class);
            assertThat(binding.getExchange()).isEqualTo(exchange.getName());
            assertThat(binding.getDestination()).isEqualTo(queue.getName());
            assertThat(binding.getDestinationType()).isEqualTo(Binding.DestinationType.QUEUE);
            assertThat(binding.getRoutingKey()).isEqualTo("courses.send-course-to-approve");
        });
    }

    /**
     * Stand-ins for the beans Spring Boot's RabbitMQ auto-configuration would provide; no broker connection is made.
     */
    @Configuration
    static class RabbitStubs {

        @Bean
        AmqpTemplate amqpTemplate() {
            return mock(AmqpTemplate.class);
        }

        @Bean(name = RabbitListenerAnnotationBeanPostProcessor.DEFAULT_RABBIT_LISTENER_CONTAINER_FACTORY_BEAN_NAME)
        RabbitListenerContainerFactory<?> rabbitListenerContainerFactory() {
            @SuppressWarnings("unchecked")
            RabbitListenerContainerFactory<MessageListenerContainer> factory = mock(RabbitListenerContainerFactory.class);
            when(factory.createListenerContainer(any(RabbitListenerEndpoint.class)))
                    .thenReturn(mock(MessageListenerContainer.class));
            return factory;
        }
    }
}
