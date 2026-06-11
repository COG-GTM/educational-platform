package com.educational.platform.courses;

import com.educational.platform.courses.course.CourseCannotBePublishedException;
import com.educational.platform.courses.course.create.CreateCourseCommandHandler;
import com.educational.platform.courses.course.publish.PublishCourseCommandHandler;
import com.educational.platform.web.handler.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Tests the publish endpoint exception path: handler throws CourseCannotBePublishedException
 * → @ExceptionHandler produces CONFLICT response with error details.
 */
@ExtendWith(MockitoExtension.class)
public class CourseControllerPublishExceptionFlowTest {

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
    void publish_handlerThrowsCourseCannotBePublished_exceptionPropagates() {
        // given
        final UUID uuid = UUID.randomUUID();
        doThrow(new CourseCannotBePublishedException(uuid)).when(publishHandler).handle(any());

        // when / then
        assertThatExceptionOfType(CourseCannotBePublishedException.class)
                .isThrownBy(() -> sut.publish(uuid));
    }

    @Test
    void onConflictException_courseCannotBePublished_returnsConflictWithUuidInMessage() {
        // given
        final UUID uuid = UUID.randomUUID();
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(uuid);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onConflictException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).hasSize(1);
        assertThat(response.getBody().errors().getFirst())
                .contains(uuid.toString())
                .contains("cannot be published");
    }
}
