package com.educational.platform.course.reviews.enrollment.create;

import com.educational.platform.course.reviews.enrollment.ReviewerEnrollment;
import com.educational.platform.course.reviews.enrollment.ReviewerEnrollmentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateReviewerEnrollmentCommandHandlerTest {

    private final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Mock
    private ReviewerEnrollmentRepository reviewerEnrollmentRepository;

    private CreateReviewerEnrollmentCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new CreateReviewerEnrollmentCommandHandler(validator, reviewerEnrollmentRepository);
    }

    @Test
    void handle_newEnrollment_enrollmentSaved() {
        // given
        final CreateReviewerEnrollmentCommand command = new CreateReviewerEnrollmentCommand(courseId, "username");
        when(reviewerEnrollmentRepository.existsByCourseIdAndUsername(courseId, "username")).thenReturn(false);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<ReviewerEnrollment> argument = ArgumentCaptor.forClass(ReviewerEnrollment.class);
        verify(reviewerEnrollmentRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("courseId", courseId)
                .hasFieldOrPropertyWithValue("username", "username");
    }

    @Test
    void handle_existingEnrollment_nothingSaved() {
        // given
        final CreateReviewerEnrollmentCommand command = new CreateReviewerEnrollmentCommand(courseId, "username");
        when(reviewerEnrollmentRepository.existsByCourseIdAndUsername(courseId, "username")).thenReturn(true);

        // when
        sut.handle(command);

        // then
        verify(reviewerEnrollmentRepository, never()).save(any());
    }

    @Test
    void handle_usernameIsBlank_constraintViolationException() {
        // given
        final CreateReviewerEnrollmentCommand command = new CreateReviewerEnrollmentCommand(courseId, " ");

        // when
        final Executable handleAction = () -> sut.handle(command);

        // then
        assertThrows(ConstraintViolationException.class, handleAction);
    }

}
