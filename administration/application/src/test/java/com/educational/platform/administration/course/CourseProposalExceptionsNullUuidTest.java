package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests exception behavior when null UUID is passed to course proposal exceptions.
 * Verifies no NPE during construction and that getMessage handles null gracefully.
 */
public class CourseProposalExceptionsNullUuidTest {

    @Test
    void alreadyApprovedException_nullUuid_messageContainsNull() {
        // when
        final CourseProposalAlreadyApprovedException exception =
                new CourseProposalAlreadyApprovedException(null);

        // then
        assertThat(exception.getMessage())
                .contains("null")
                .contains("cannot be approved");
    }

    @Test
    void alreadyDeclinedException_nullUuid_messageContainsNull() {
        // when
        final CourseProposalAlreadyDeclinedException exception =
                new CourseProposalAlreadyDeclinedException(null);

        // then
        assertThat(exception.getMessage())
                .contains("null")
                .contains("cannot be declined");
    }

    @Test
    void alreadyApprovedException_nullUuid_noNPE() {
        // construction should not throw
        final CourseProposalAlreadyApprovedException exception =
                new CourseProposalAlreadyApprovedException(null);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void alreadyDeclinedException_nullUuid_noNPE() {
        // construction should not throw
        final CourseProposalAlreadyDeclinedException exception =
                new CourseProposalAlreadyDeclinedException(null);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
