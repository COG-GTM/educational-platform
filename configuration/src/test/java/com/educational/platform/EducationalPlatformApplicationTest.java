package com.educational.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

class EducationalPlatformApplicationTest {

    @Test
    void applicationClass_doesNotHaveEnableAsync() {
        assertThat(EducationalPlatformApplication.class.getAnnotation(EnableAsync.class)).isNull();
    }

    @Test
    void applicationClass_doesNotHaveEnableRetry() {
        assertThat(EducationalPlatformApplication.class.getAnnotation(EnableRetry.class)).isNull();
    }

    @Test
    void applicationClass_hasSpringBootApplicationAnnotation() {
        assertThat(EducationalPlatformApplication.class.getAnnotation(SpringBootApplication.class)).isNotNull();
    }

    @Test
    void applicationClass_hasPropertySourceAnnotation() {
        PropertySource propertySource = EducationalPlatformApplication.class.getAnnotation(PropertySource.class);
        assertThat(propertySource).isNotNull();
        assertThat(propertySource.value()).contains("application-security.properties");
    }

    @Test
    void applicationClass_doesNotHaveEnableRetry() {
        assertThat(EducationalPlatformApplication.class.getAnnotation(EnableRetry.class)).isNull();
    }
}
