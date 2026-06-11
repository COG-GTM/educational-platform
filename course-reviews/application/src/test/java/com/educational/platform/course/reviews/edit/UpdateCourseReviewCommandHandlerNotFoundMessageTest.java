package com.educational.platform.course.reviews.edit;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.reviews.CourseReviewRepository;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateCourseReviewCommandHandlerNotFoundMessageTest {

    @Mock
    private CourseReviewRepository courseReviewRepository;

    private UpdateCourseReviewCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new UpdateCourseReviewCommandHandler(validator, courseReviewRepository);
    }

    @Test
    void handle_reviewNotFound_exceptionMessageContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 3.0, "comment");
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(handle)
                .withMessageContaining(uuid.toString());
    }

    @Test
    void handle_reviewNotFound_exceptionMessageContainsCourseReviewLabel() {
        // given
        final UUID uuid = UUID.randomUUID();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 3.0, "comment");
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(handle)
                .withMessageContaining("Course Review");
    }
}
