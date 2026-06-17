package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseReviewFactoryTest {

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
    void createFrom_validCourseReview_courseReviewCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(uuid, 4.0, "comment");

        final CreateReviewableCourseCommand createCourseProposalCommand = new CreateReviewableCourseCommand(uuid);
        final ReviewableCourse correspondingReviewableCourse = new ReviewableCourse(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingReviewableCourse, "id", 11);
        ReflectionTestUtils.setField(correspondingReviewableCourse, "originalCourseId", uuid);
        when(reviewableCourseRepository.findByOriginalCourseId(uuid)).thenReturn(Optional.of(correspondingReviewableCourse));

        final CreateReviewerCommand createReviewerCommand = new CreateReviewerCommand("username");
        final Reviewer correspondingReviewer = new Reviewer(createReviewerCommand);
        ReflectionTestUtils.setField(correspondingReviewer, "id", 22);
        ReflectionTestUtils.setField(correspondingReviewer, "username", "username");
        when(currentUserAsReviewer.userAsReviewer()).thenReturn(correspondingReviewer);

        // when
        final CourseReview courseReview = sut.createFrom(command);

        // then - the factory copies the command's rating/comment and wires the resolved reviewable course
        // (id 11) and reviewer (id 22) as the foreign-key references the optimistic-lock isolation tests rely
        // on being independent. It also leaves the @Version this PR added unmanaged: the factory never seeds a
        // version, so a freshly constructed (transient) review still has a null version here - Hibernate
        // initialises it to 0 on persist. No other test pins the factory's treatment of these references or
        // the new version field; the slice tests assert the resulting persisted values, not this construction.
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("comment"))
                .hasFieldOrPropertyWithValue("course", 11)
                .hasFieldOrPropertyWithValue("reviewer", 22);
        assertThat(ReflectionTestUtils.getField(courseReview, "version")).isNull();
    }


    @Test
    void createFrom_courseIdIsNull_constraintViolationException() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(null, 4.0, "comment");

        // when
        final Executable createAction = () -> sut.createFrom(command);

        // then
        assertThrows(ConstraintViolationException.class, createAction);
    }


    @ParameterizedTest
    @ValueSource(doubles = {-1, 6})
    void createFrom_invalidRating_constraintViolationException(double rating) {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(uuid, rating, "comment");

        // when
        final Executable createAction = () -> sut.createFrom(command);

        // then
        assertThrows(ConstraintViolationException.class, createAction);
    }

    @Test
    void createFrom_emptyRating_constraintViolationException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(uuid, null, "comment");

        // when
        final Executable createAction = () -> sut.createFrom(command);

        // then
        assertThrows(ConstraintViolationException.class, createAction);
    }

}
