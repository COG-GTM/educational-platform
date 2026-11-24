package com.educational.platform.course.reviews.create;

import com.educational.platform.course.reviews.CourseRating;
import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewFactory;
import com.educational.platform.course.reviews.CourseReviewRepository;
import com.educational.platform.course.reviews.CurrentUserAsReviewer;
import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewCourseCommandHandlerTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static ValidatorFactory validatorFactory;

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private CurrentUserAsReviewer currentUserAsReviewer;

    @Mock
    private ReviewableCourseRepository reviewableCourseRepository;

    private ReviewCourseCommandHandler sut;

    @BeforeAll
    static void openValidatorFactory() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        var factory = new CourseReviewFactory(validatorFactory.getValidator(), currentUserAsReviewer,
                reviewableCourseRepository);
        sut = new ReviewCourseCommandHandler(courseReviewRepository, factory);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 4.5, 4.999999999999999, 5.0})
    void handle_validRating_savesReviewAndReturnsItsIdentifier(double rating) {
        // given
        var course = new ReviewableCourse(new CreateReviewableCourseCommand(COURSE_ID));
        ReflectionTestUtils.setField(course, "id", 11);
        when(reviewableCourseRepository.findByOriginalCourseId(COURSE_ID)).thenReturn(Optional.of(course));

        var reviewer = new Reviewer(new CreateReviewerCommand("username"));
        ReflectionTestUtils.setField(reviewer, "id", 22);
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        var command = new ReviewCourseCommand(COURSE_ID, rating, "comment");

        // when
        UUID reviewId = sut.handle(command);

        // then
        var savedReview = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(savedReview.capture());
        assertThat(savedReview.getValue()).hasFieldOrPropertyWithValue("rating", new CourseRating(rating));
        assertThat(reviewId).isNotNull().isEqualTo(savedReview.getValue().toIdentifier());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(doubles = {-0.01, -Double.MIN_VALUE, 5.000000000000001, 5.01, 6.0})
    void handle_invalidRating_rejectsBeforeLookupOrSave(Double rating) {
        // given
        var command = new ReviewCourseCommand(COURSE_ID, rating, "comment");

        // when
        var exception = assertThrows(ConstraintViolationException.class, () -> sut.handle(command));

        // then
        assertThat(exception.getConstraintViolations()).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("rating");
            assertThat(violation.getInvalidValue()).isEqualTo(rating);
        });
        verifyNoInteractions(reviewableCourseRepository, currentUserAsReviewer, courseReviewRepository);
    }
}
