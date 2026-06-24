package com.educational.platform;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

class EducationalPlatformApplicationTest {

    @Test
    void class_doesNotHaveEnableAsyncAnnotation() {
        // @EnableAsync was moved to AsyncConfig to co-locate with @EnableRetry ordering
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(EnableAsync.class)).isFalse();
    }

}
