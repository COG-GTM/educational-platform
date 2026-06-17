package com.educational.platform.course.reviews.reviewer.create;

import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.ReviewerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the reviewer create use case in isolation. A reviewer's optimistic-lock {@code version} lifecycle
 * (added by this PR) begins at the single persist this handler performs: that insert is the row Hibernate then
 * initialises to version 0. {@link com.educational.platform.course.reviews.create.ReviewCourseCommandHandlerTest}
 * pins this single-persist contract for the {@code CourseReview} aggregate; the equivalent isolated test for the
 * reviewer create handler was missing. The repository is mocked, so the focus is the handler's save contract: it
 * persists exactly one reviewer carrying the command's username and hands Hibernate a transient instance whose
 * version it has not pre-set (the slice tests assert the resulting 0).
 */
@ExtendWith(MockitoExtension.class)
public class CreateReviewerCommandHandlerTest {

    @Mock
    private ReviewerRepository reviewerRepository;

    private CreateReviewerCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new CreateReviewerCommandHandler(reviewerRepository);
    }

    @Test
    void handle_validCommand_reviewerPersistedWithUsernameAndUnmanagedVersion() {
        // given - a create command for a new reviewer
        final CreateReviewerCommand command = new CreateReviewerCommand("new-reviewer");

        // when
        sut.handle(command);

        // then - the handler persists exactly one reviewer built from the command. The persisted instance still
        // carries a null @Version at the point of save: the handler hands Hibernate a fresh row whose version
        // lifecycle starts there (Hibernate writes 0 on insert) and does not pre-set it. No other test exercises
        // this handler in isolation - the slice asserts the resulting version 0 but not the single-persist contract.
        final ArgumentCaptor<Reviewer> argument = ArgumentCaptor.forClass(Reviewer.class);
        verify(reviewerRepository).save(argument.capture());
        final Reviewer persisted = argument.getValue();
        assertThat(ReflectionTestUtils.getField(persisted, "username")).isEqualTo("new-reviewer");
        assertThat(ReflectionTestUtils.getField(persisted, "version")).isNull();
    }
}
