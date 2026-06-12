package com.educational.platform.courses.course.approve;

import com.educational.platform.courses.course.*;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import com.educational.platform.courses.teacher.Teacher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendCourseToApproveCommandHandlerSuccessTest {

    private CourseFactory courseFactory;

    @Mock
    private CurrentUserAsTeacher currentUserAsTeacher;

    @Mock
    private CourseRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SendCourseToApproveCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        courseFactory = new CourseFactory(validator, currentUserAsTeacher);
    }

    @Test
    void handle_draftCourse_statusChangedToWaitingForApproval() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveCommand command = new SendCourseToApproveCommand(uuid);

        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course correspondingCourse = courseFactory.createFrom(createCourseCommand);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourse));

        // when
        sut.handle(command);

        // then
        assertThat(correspondingCourse)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void handle_draftCourse_integrationEventPublished() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveCommand command = new SendCourseToApproveCommand(uuid);

        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course correspondingCourse = courseFactory.createFrom(createCourseCommand);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourse));

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<SendCourseToApproveIntegrationEvent> eventCaptor =
                ArgumentCaptor.forClass(SendCourseToApproveIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().courseId()).isEqualTo(uuid);
    }

    @Test
    void handle_declinedCourse_statusChangedToWaitingForApproval() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveCommand command = new SendCourseToApproveCommand(uuid);

        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course correspondingCourse = courseFactory.createFrom(createCourseCommand);
        correspondingCourse.decline();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourse));

        // when
        sut.handle(command);

        // then
        assertThat(correspondingCourse)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }
}
