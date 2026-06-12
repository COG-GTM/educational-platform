package com.educational.platform.courses;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.courses.course.CourseCannotBePublishedException;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateCourseCommandHandler;
import com.educational.platform.courses.course.publish.PublishCourseCommand;
import com.educational.platform.courses.course.publish.PublishCourseCommandHandler;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseControllerEdgeCaseTest {

    @Mock
    private CreateCourseCommandHandler createCourseCommandHandler;

    @Mock
    private PublishCourseCommandHandler publishCourseCommandHandler;

    @InjectMocks
    private CourseController sut;

    @Test
    void publish_handlerThrowsResourceNotFound_exceptionPropagated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new ResourceNotFoundException("Course with uuid: " + uuid + " not found"))
                .when(publishCourseCommandHandler).handle(any(PublishCourseCommand.class));

        // when
        final ThrowableAssert.ThrowingCallable publish = () -> sut.publish(uuid);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(publish);
    }

    @Test
    void publish_handlerThrowsCourseCannotBePublished_exceptionPropagated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new CourseCannotBePublishedException(uuid))
                .when(publishCourseCommandHandler).handle(any(PublishCourseCommand.class));

        // when
        final ThrowableAssert.ThrowingCallable publish = () -> sut.publish(uuid);

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(publish);
    }

    @Test
    void create_handlerThrowsRuntimeException_exceptionPropagated() {
        // given
        final CreateCourseRequest request = new CreateCourseRequest("Java Basics", "Learn Java");
        when(createCourseCommandHandler.handle(any(CreateCourseCommand.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // when
        final ThrowableAssert.ThrowingCallable create = () -> sut.create(request);

        // then
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(create);
    }

    @Test
    void create_handlerReturnsNull_responseContainsNullUuid() {
        // given
        final CreateCourseRequest request = new CreateCourseRequest("Java Basics", "Learn Java");
        when(createCourseCommandHandler.handle(any(CreateCourseCommand.class))).thenReturn(null);

        // when
        final CreatedCourseResponse result = sut.create(request);

        // then
        assertThat(result.uuid()).isNull();
    }

    @Test
    void publish_handlerCalledOnce_noAdditionalInteractions() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        sut.publish(uuid);

        // then
        verify(publishCourseCommandHandler, times(1)).handle(any(PublishCourseCommand.class));
        verifyNoInteractions(createCourseCommandHandler);
    }

    @Test
    void create_handlerCalledOnce_noAdditionalInteractions() {
        // given
        final CreateCourseRequest request = new CreateCourseRequest("Java", "Description");
        final UUID expectedUuid = UUID.randomUUID();
        when(createCourseCommandHandler.handle(any(CreateCourseCommand.class))).thenReturn(expectedUuid);

        // when
        sut.create(request);

        // then
        verify(createCourseCommandHandler, times(1)).handle(any(CreateCourseCommand.class));
        verifyNoInteractions(publishCourseCommandHandler);
    }
}
