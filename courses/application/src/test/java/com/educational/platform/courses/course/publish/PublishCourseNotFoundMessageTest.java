package com.educational.platform.courses.course.publish;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.courses.course.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link PublishCourseCommandHandler} includes the
 * UUID in the {@link ResourceNotFoundException} message when the course is not found.
 */
@ExtendWith(MockitoExtension.class)
public class PublishCourseNotFoundMessageTest {

    @Mock
    private CourseRepository repository;

    private PublishCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new PublishCourseCommandHandler(repository);
    }

    @Test
    void handle_courseNotFound_exceptionMessageContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440099");
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> sut.handle(new PublishCourseCommand(uuid)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(uuid.toString());
    }
}
