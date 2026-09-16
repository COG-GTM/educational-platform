package com.educational.platform.courses.bridge;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.context.event.EventListener;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class OutboundIntegrationEventBridgeTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Mock
    private AmqpTemplate amqpTemplate;

    @Test
    void courseApprovedByAdmin_sentToBrokerTopic() {
        var bridge = new OutboundIntegrationEventBridge(amqpTemplate);

        bridge.on(new CourseApprovedByAdminIntegrationEvent(COURSE_ID));

        var message = ArgumentCaptor.forClass(Message.class);
        verify(amqpTemplate).send(eq(IntegrationEventTopics.EXCHANGE), eq(IntegrationEventTopics.COURSE_APPROVED_BY_ADMIN), message.capture());
        assertThat(new String(message.getValue().getBody(), StandardCharsets.UTF_8))
                .isEqualTo("{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\"}");
        assertThat(message.getValue().getMessageProperties().getContentType()).isEqualTo("application/json");
    }

    @Test
    void studentEnrolledToCourse_sentToBrokerTopic() {
        var bridge = new OutboundIntegrationEventBridge(amqpTemplate);

        bridge.on(new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student"));

        var message = ArgumentCaptor.forClass(Message.class);
        verify(amqpTemplate).send(eq(IntegrationEventTopics.EXCHANGE), eq(IntegrationEventTopics.STUDENT_ENROLLED_TO_COURSE), message.capture());
        assertThat(new String(message.getValue().getBody(), StandardCharsets.UTF_8))
                .isEqualTo("{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\",\"username\":\"student\"}");
    }

    @Test
    void userCreated_sentToBrokerTopic() {
        var bridge = new OutboundIntegrationEventBridge(amqpTemplate);

        bridge.on(new UserCreatedIntegrationEvent("teacher", "teacher@example.com"));

        var message = ArgumentCaptor.forClass(Message.class);
        verify(amqpTemplate).send(eq(IntegrationEventTopics.EXCHANGE), eq(IntegrationEventTopics.USER_CREATED), message.capture());
        assertThat(new String(message.getValue().getBody(), StandardCharsets.UTF_8))
                .isEqualTo("{\"username\":\"teacher\",\"email\":\"teacher@example.com\"}");
    }

    @Test
    void userCreated_nullEmail_jsonNull() {
        var bridge = new OutboundIntegrationEventBridge(amqpTemplate);

        bridge.on(new UserCreatedIntegrationEvent("teacher", null));

        var message = ArgumentCaptor.forClass(Message.class);
        verify(amqpTemplate).send(eq(IntegrationEventTopics.EXCHANGE), eq(IntegrationEventTopics.USER_CREATED), message.capture());
        assertThat(new String(message.getValue().getBody(), StandardCharsets.UTF_8))
                .isEqualTo("{\"username\":\"teacher\",\"email\":null}");
    }

    @Test
    void courseRatingRecalculated_sentToBrokerTopic() {
        var bridge = new OutboundIntegrationEventBridge(amqpTemplate);

        bridge.on(new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5));

        var message = ArgumentCaptor.forClass(Message.class);
        verify(amqpTemplate).send(eq(IntegrationEventTopics.EXCHANGE), eq(IntegrationEventTopics.COURSE_RATING_RECALCULATED), message.capture());
        assertThat(new String(message.getValue().getBody(), StandardCharsets.UTF_8))
                .isEqualTo("{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\",\"rating\":4.5}");
    }

    @Test
    void anyEvent_persistentJsonMessageAndExactlyOneSend() {
        var bridge = new OutboundIntegrationEventBridge(amqpTemplate);

        bridge.on(new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 0.0));

        var message = ArgumentCaptor.forClass(Message.class);
        verify(amqpTemplate).send(anyString(), anyString(), message.capture());
        verifyNoMoreInteractions(amqpTemplate);
        var properties = message.getValue().getMessageProperties();
        assertThat(properties.getContentType()).isEqualTo("application/json");
        assertThat(properties.getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(new String(message.getValue().getBody(), StandardCharsets.UTF_8))
                .isEqualTo("{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\",\"rating\":0.0}");
    }

    @Test
    void brokerFailure_propagatedToEventPublisher() {
        var bridge = new OutboundIntegrationEventBridge(amqpTemplate);
        doThrow(new IllegalStateException("broker down")).when(amqpTemplate).send(anyString(), anyString(), any(Message.class));

        assertThatThrownBy(() -> bridge.on(new CourseApprovedByAdminIntegrationEvent(COURSE_ID)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("broker down");
    }

    @Test
    void sendCourseToApprove_notBridgedOutbound() {
        var listenedEventTypes = Arrays.stream(OutboundIntegrationEventBridge.class.getMethods())
                .filter(method -> method.isAnnotationPresent(EventListener.class))
                .map(method -> method.getParameterTypes()[0])
                .toList();

        assertThat(listenedEventTypes).containsExactlyInAnyOrder(
                CourseApprovedByAdminIntegrationEvent.class,
                StudentEnrolledToCourseIntegrationEvent.class,
                UserCreatedIntegrationEvent.class,
                CourseRatingRecalculatedIntegrationEvent.class);
        verify(amqpTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }
}
