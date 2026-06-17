package com.educational.platform.course.reviews.course.create;

import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the reviewable-course create use case in isolation. A reviewable course's optimistic-lock
 * {@code version} lifecycle (added by this PR) begins at the single persist this handler performs: that insert is
 * the row Hibernate then initialises to version 0. {@link com.educational.platform.course.reviews.create.ReviewCourseCommandHandlerTest}
 * pins this single-persist contract for the {@code CourseReview} aggregate; the equivalent isolated test for the
 * reviewable-course create handler was missing - and because {@link ReviewableCourse} is excluded from the
 * migrated-schema slice, the create handler's hands-off treatment of the version is otherwise unverified outside
 * the Hibernate-generated slice. The repository is mocked, so the focus is the handler's save contract: it persists
 * exactly one reviewable course carrying the command's uuid and hands Hibernate a transient instance whose version
 * it has not pre-set (the slice tests assert the resulting 0).
 */
@ExtendWith(MockitoExtension.class)
public class CreateReviewableCourseCommandHandlerTest {

    @Mock
    private ReviewableCourseRepository reviewableCourseRepository;

    private CreateReviewableCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new CreateReviewableCourseCommandHandler(reviewableCourseRepository);
    }

    @Test
    void handle_validCommand_reviewableCoursePersistedWithOriginalCourseIdAndUnmanagedVersion() {
        // given - a create command for a new reviewable course
        final UUID originalCourseId = UUID.randomUUID();
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(originalCourseId);

        // when
        sut.handle(command);

        // then - the handler persists exactly one reviewable course built from the command. The persisted instance
        // still carries a null @Version at the point of save: the handler hands Hibernate a fresh row whose version
        // lifecycle starts there (Hibernate writes 0 on insert) and does not pre-set it. No other test exercises
        // this handler in isolation - the slice asserts the resulting version 0 but not the single-persist contract.
        final ArgumentCaptor<ReviewableCourse> argument = ArgumentCaptor.forClass(ReviewableCourse.class);
        verify(reviewableCourseRepository).save(argument.capture());
        final ReviewableCourse persisted = argument.getValue();
        assertThat(ReflectionTestUtils.getField(persisted, "originalCourseId")).isEqualTo(originalCourseId);
        assertThat(ReflectionTestUtils.getField(persisted, "version")).isNull();
    }
}
