package com.educational.platform.common.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FailedEventStatusTest {

    @Test
    void declaresExactlyFailedAndResolvedValues() {
        // expect
        assertThat(FailedEventStatus.values())
                .containsExactly(FailedEventStatus.FAILED, FailedEventStatus.RESOLVED);
    }

    @Test
    void valueOf_resolvesByName() {
        // expect
        assertThat(FailedEventStatus.valueOf("FAILED")).isEqualTo(FailedEventStatus.FAILED);
        assertThat(FailedEventStatus.valueOf("RESOLVED")).isEqualTo(FailedEventStatus.RESOLVED);
    }
}
