package com.educational.platform.courses;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateCourseCommandHandler;
import com.educational.platform.courses.course.publish.PublishCourseCommand;
import com.educational.platform.courses.course.publish.PublishCourseCommandHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseControllerCommandMappingTest {

    @Mock
    private CreateCourseCommandHandler createCourseCommandHandler;

    @Mock
    private PublishCourseCommandHandler publishCourseCommandHandler;

    @InjectMocks
    private CourseController sut;

    @Test
    void create_validRequest_commandContainsCorrectNameAndDescription() {
        // given
        final CreateCourseRequest request = new CreateCourseRequest("Java Basics", "Learn Java fundamentals");
        when(createCourseCommandHandler.handle(any(CreateCourseCommand.class))).thenReturn(UUID.randomUUID());

        // when
        sut.create(request);

        // then
        final ArgumentCaptor<CreateCourseCommand> argument = ArgumentCaptor.forClass(CreateCourseCommand.class);
        verify(createCourseCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().name()).isEqualTo("Java Basics");
        assertThat(argument.getValue().description()).isEqualTo("Learn Java fundamentals");
    }

    @Test
    void publish_validUuid_commandContainsCorrectUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        sut.publish(uuid);

        // then
        final ArgumentCaptor<PublishCourseCommand> argument = ArgumentCaptor.forClass(PublishCourseCommand.class);
        verify(publishCourseCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().uuid()).isEqualTo(uuid);
    }

    @Test
    void create_validRequest_returnsResponseWithUuid() {
        // given
        final UUID expectedUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseRequest request = new CreateCourseRequest("Testing", "Learn testing");
        when(createCourseCommandHandler.handle(any(CreateCourseCommand.class))).thenReturn(expectedUuid);

        // when
        final CreatedCourseResponse response = sut.create(request);

        // then
        assertThat(response.uuid()).isEqualTo(expectedUuid);
    }
}
