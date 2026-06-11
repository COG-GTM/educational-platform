package com.educational.platform.course.reviews;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseReviewFactoryReviewerNotFoundTest {

    @Mock
    private ReviewableCourseRepository reviewableCourseRepository;

    @Mock
    private CurrentUserAsReviewer currentUserAsReviewer;

    private CourseReviewFactory sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new CourseReviewFactory(validator, currentUserAsReviewer, reviewableCourseRepository);
    }

    @Test
    void createFrom_reviewerResolutionThrows_propagatesException() {
        // given
        final UUID courseId = UUID.randomUUID();
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");
        final ReviewableCourse course = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(course, "id", 10);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));
        when(currentUserAsReviewer.userAsReviewer()).thenThrow(
                new RelatedResourceIsNotResolvedException("Reviewer not found"));

        // when / then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class)
                .isThrownBy(() -> sut.createFrom(command))
                .withMessageContaining("Reviewer not found");
    }

    @Test
    void createFrom_reviewerReturnsNull_throwsNullPointerException() {
        // given
        final UUID courseId = UUID.randomUUID();
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");
        final ReviewableCourse course = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));
        ReflectionTestUtils.setField(course, "id", 10);
        when(reviewableCourseRepository.findByOriginalCourseId(courseId)).thenReturn(Optional.of(course));
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(null);

        // when / then
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }
}
