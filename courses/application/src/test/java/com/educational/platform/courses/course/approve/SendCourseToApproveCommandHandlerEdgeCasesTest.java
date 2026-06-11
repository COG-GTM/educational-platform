package com.educational.platform.courses.course.approve;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.courses.course.*;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SendCourseToApproveCommandHandlerEdgeCasesTest {

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
    void handle_courseNotFound_resourceNotFoundException() {
        // given
        final UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(new SendCourseToApproveCommand(uuid));

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
    }

    @Test
    void handle_courseAlreadyApproved_courseAlreadyApprovedException() {
        // given
        final UUID uuid = UUID.randomUUID();
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        ReflectionTestUtils.setField(course, "approvalStatus", ApprovalStatus.APPROVED);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(new SendCourseToApproveCommand(uuid));

        // then
        assertThatExceptionOfType(CourseAlreadyApprovedException.class).isThrownBy(handle);
    }
}
