package com.educational.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

class EducationalPlatformApplicationTest {

    @Test
    void applicationClass_doesNotHaveEnableAsync() {
        assertThat(EducationalPlatformApplication.class.getAnnotation(EnableAsync.class)).isNull();
    }

    @Test
    void applicationClass_hasSpringBootApplicationAnnotation() {
        assertThat(EducationalPlatformApplication.class.getAnnotation(SpringBootApplication.class)).isNotNull();
    }
}
