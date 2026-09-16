package com.educational.platform.courses.bridge;

import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class InboundIntegrationEventBridgeTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void onSendCourseToApprove_pythonPayload_sendCourseToApproveEventRepublished() {
        var bridge = new InboundIntegrationEventBridge(eventPublisher);

        bridge.onSendCourseToApprove("{\"courseId\": \"123e4567-e89b-12d3-a456-426655440001\"}");

        verify(eventPublisher).publishEvent(new SendCourseToApproveIntegrationEvent(COURSE_ID));
    }

    @Test
    void onSendCourseToApprove_compactPayloadWithExtraKeys_courseIdExtracted() {
        var bridge = new InboundIntegrationEventBridge(eventPublisher);

        bridge.onSendCourseToApprove("{\"occurredOn\":\"2026-01-01\",\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\",\"x\":1}");

        verify(eventPublisher).publishEvent(new SendCourseToApproveIntegrationEvent(COURSE_ID));
    }

    @Test
    void onSendCourseToApprove_upperCaseUuid_normalised() {
        var bridge = new InboundIntegrationEventBridge(eventPublisher);

        bridge.onSendCourseToApprove("{\"courseId\":\"123E4567-E89B-12D3-A456-426655440001\"}");

        verify(eventPublisher).publishEvent(new SendCourseToApproveIntegrationEvent(COURSE_ID));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "",
            "not json",
            "{\"username\":\"teacher\"}",
            "{\"courseId\":null}",
            "{\"courseId\":\"\"}",
            "{\"courseId\":\"not-a-uuid\"}",
            "{\"courseId\":\"123e4567-e89b-12d3-a456-42665544000\"}",
            "{\"course_id\":\"123e4567-e89b-12d3-a456-426655440001\"}",
            "{\"courseId\":\"------------------------------------\"}",
            "{\"courseId\":\"123e4567e89b12d3a456426655440001----\"}",
            "{\"event\":{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\"}}",
            "{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\"} {\"courseId\":\"123e4567-e89b-12d3-a456-426655440002\"}"
    })
    void onSendCourseToApprove_payloadWithoutValidCourseId_rejectedWithoutRequeue(String json) {
        var bridge = new InboundIntegrationEventBridge(eventPublisher);

        assertThatThrownBy(() -> bridge.onSendCourseToApprove(json))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("courses.send-course-to-approve message without courseId")
                .hasMessageContaining(json);

        verifyNoInteractions(eventPublisher);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"nested\":{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\"}}",
            "[{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\"}]",
            "{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\"} trailing",
            "{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\"",
            "\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\"",
            "{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\",\"x\":[1]}"
    })
    void onSendCourseToApprove_notAFlatJsonObject_rejectedWithoutRequeue(String json) {
        var bridge = new InboundIntegrationEventBridge(eventPublisher);

        assertThatThrownBy(() -> bridge.onSendCourseToApprove(json))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining(json);

        verifyNoInteractions(eventPublisher);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"courseId\":\"------------------------------------\"}",
            "{\"courseId\":\"123e4567e89b12d3a456426655440001abcd\"}",
            "{\"courseId\":\"123e4567-e89b-12d3-a456426655440001-\"}"
    })
    void onSendCourseToApprove_thirtySixCharactersButNotAUuid_rejectedWithoutRequeueInsteadOfIllegalArgument(
            String json) {
        var bridge = new InboundIntegrationEventBridge(eventPublisher);

        assertThatThrownBy(() -> bridge.onSendCourseToApprove(json))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .isNotInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(json);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void onSendCourseToApprove_surroundingWhitespace_courseIdExtracted() {
        var bridge = new InboundIntegrationEventBridge(eventPublisher);

        bridge.onSendCourseToApprove(" \n{\"courseId\": \"123e4567-e89b-12d3-a456-426655440001\"}\n");

        verify(eventPublisher).publishEvent(new SendCourseToApproveIntegrationEvent(COURSE_ID));
    }

    @Test
    void onSendCourseToApprove_publisherFails_exceptionPropagatesForBrokerRetry() {
        var bridge = new InboundIntegrationEventBridge(eventPublisher);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        doThrow(new IllegalStateException("context closed")).when(eventPublisher).publishEvent(event);

        assertThatThrownBy(() -> bridge.onSendCourseToApprove("{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\"}"))
                .isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void onSendCourseToApprove_listensOnMonolithQueueBoundToSharedRoutingKey() throws NoSuchMethodException {
        var listener = InboundIntegrationEventBridge.class
                .getMethod("onSendCourseToApprove", String.class)
                .getAnnotation(RabbitListener.class);

        assertThat(listener).isNotNull();
        assertThat(listener.queues()).containsExactly("java-monolith.courses.send-course-to-approve");
        assertThat(IntegrationEventTopics.MONOLITH_SEND_COURSE_TO_APPROVE_QUEUE)
                .isEqualTo("java-monolith." + IntegrationEventTopics.SEND_COURSE_TO_APPROVE);
    }
}
