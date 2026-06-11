package com.educational.platform.courses.course.approve;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.courses.course.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link SendCourseToApproveCommandHandler} includes the
 * UUID in the {@link ResourceNotFoundException} message when the course is not found.
 */
@ExtendWith(MockitoExtension.class)
public class SendCourseToApproveNotFoundMessageTest {

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
    void handle_courseNotFound_exceptionMessageContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440099");
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> sut.handle(new SendCourseToApproveCommand(uuid)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(uuid.toString());
    }
}
