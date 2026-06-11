package com.educational.platform.courses;

import com.educational.platform.courses.course.CourseCannotBePublishedException;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateCourseCommandHandler;
import com.educational.platform.courses.course.publish.PublishCourseCommand;
import com.educational.platform.courses.course.publish.PublishCourseCommandHandler;
import com.educational.platform.web.handler.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseControllerUnitTest {

    @Mock
    private CreateCourseCommandHandler createHandler;

    @Mock
    private PublishCourseCommandHandler publishHandler;

    private CourseController sut;

    @BeforeEach
    void setUp() {
        sut = new CourseController(createHandler, publishHandler);
    }

    @Test
    void create_delegatesToCreateHandler_returnsResponse() {
        // given
        final UUID uuid = UUID.randomUUID();
        when(createHandler.handle(any())).thenReturn(uuid);
        final CreateCourseRequest request = new CreateCourseRequest("Course", "Description");

        // when
        final CreatedCourseResponse result = sut.create(request);

        // then
        assertThat(result.uuid()).isEqualTo(uuid);
        final ArgumentCaptor<CreateCourseCommand> captor = ArgumentCaptor.forClass(CreateCourseCommand.class);
        verify(createHandler).handle(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Course");
        assertThat(captor.getValue().description()).isEqualTo("Description");
    }

    @Test
    void create_mapsRequestFieldsToCommand() {
        // given
        when(createHandler.handle(any())).thenReturn(UUID.randomUUID());
        final CreateCourseRequest request = new CreateCourseRequest("Math 101", "Intro to mathematics");

        // when
        sut.create(request);

        // then
        final ArgumentCaptor<com.educational.platform.courses.course.create.CreateCourseCommand> captor =
                ArgumentCaptor.forClass(com.educational.platform.courses.course.create.CreateCourseCommand.class);
        verify(createHandler).handle(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Math 101");
        assertThat(captor.getValue().description()).isEqualTo("Intro to mathematics");
    }

    @Test
    void publish_delegatesToPublishHandler() {
        // given
        final UUID uuid = UUID.randomUUID();

        // when
        sut.publish(uuid);

        // then
        final ArgumentCaptor<PublishCourseCommand> captor = ArgumentCaptor.forClass(PublishCourseCommand.class);
        verify(publishHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(uuid);
    }

    @Test
    void onConflictException_returnsConflictStatus() {
        // given
        final UUID uuid = UUID.randomUUID();
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(uuid);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onConflictException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).anyMatch(e -> e.contains(uuid.toString()));
    }
}
