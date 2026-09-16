package com.educational.platform.courses.bridge;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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
}
