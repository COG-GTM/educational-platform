package com.educational.platform.courses.course.approve;

import com.educational.platform.courses.course.ApprovalStatus;
import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that sending a course to approve from WAITING_FOR_APPROVAL state
 * succeeds (idempotent-like behavior since the guard only blocks APPROVED).
 * Also tests sending from DECLINED state re-transitions correctly.
 */
@ExtendWith(MockitoExtension.class)
public class SendCourseToApproveCommandHandlerWaitingStateTest {

    @Mock
    private CourseRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SendCourseToApproveCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new SendCourseToApproveCommandHandler(repository, eventPublisher);
    }

    @Test
    void handle_courseInWaitingForApprovalState_succeedsAndPublishesEvent() {
        // given
        final UUID uuid = UUID.randomUUID();
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        course.sendToApprove(); // moves to WAITING_FOR_APPROVAL
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when
        sut.handle(new SendCourseToApproveCommand(uuid));

        // then — course stays in WAITING_FOR_APPROVAL, event is published
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
        final ArgumentCaptor<SendCourseToApproveIntegrationEvent> captor =
                ArgumentCaptor.forClass(SendCourseToApproveIntegrationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("courseId", uuid);
    }

    @Test
    void handle_courseInDeclinedState_transitionsToWaitingAndPublishesEvent() {
        // given
        final UUID uuid = UUID.randomUUID();
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        course.decline(); // set to DECLINED
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when
        sut.handle(new SendCourseToApproveCommand(uuid));

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
        final ArgumentCaptor<SendCourseToApproveIntegrationEvent> captor =
                ArgumentCaptor.forClass(SendCourseToApproveIntegrationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("courseId", uuid);
    }
}
