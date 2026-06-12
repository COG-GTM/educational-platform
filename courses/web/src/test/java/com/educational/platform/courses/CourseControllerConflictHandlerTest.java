package com.educational.platform.courses;

import com.educational.platform.courses.course.CourseCannotBePublishedException;
import com.educational.platform.courses.course.create.CreateCourseCommandHandler;
import com.educational.platform.courses.course.publish.PublishCourseCommandHandler;
import com.educational.platform.web.handler.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CourseControllerConflictHandlerTest {

    @Mock
    private CreateCourseCommandHandler createCourseCommandHandler;

    @Mock
    private PublishCourseCommandHandler publishCourseCommandHandler;

    @InjectMocks
    private CourseController sut;

    @Test
    void onConflictException_courseCannotBePublished_returns409() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(uuid);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onConflictException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void onConflictException_courseCannotBePublished_responseBodyContainsMessage() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(uuid);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onConflictException(exception);

        // then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).contains(exception.getMessage());
    }

    @Test
    void create_validRequest_returnsCreatedCourseResponse() {
        // given
        final UUID expectedUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseRequest request = new CreateCourseRequest("Java Basics", "Learn Java fundamentals");
        org.mockito.Mockito.when(createCourseCommandHandler.handle(org.mockito.ArgumentMatchers.any()))
                .thenReturn(expectedUuid);

        // when
        final CreatedCourseResponse response = sut.create(request);

        // then
        assertThat(response.uuid()).isEqualTo(expectedUuid);
    }
}
