package com.educational.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

class EducationalPlatformApplicationTest {

    @Test
    void class_doesNotHaveEnableAsyncAnnotation() {
        // @EnableAsync was moved to AsyncConfig to co-locate with @EnableRetry ordering
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(EnableAsync.class)).isFalse();
    }

    @Test
    void class_hasSpringBootApplicationAnnotation() {
        // then
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
    }

    @Test
    void class_hasPropertySourceAnnotationWithSecurityProperties() {
        // when
        PropertySource propertySource = EducationalPlatformApplication.class.getAnnotation(PropertySource.class);

        // then
        assertThat(propertySource).isNotNull();
        assertThat(propertySource.value()).containsExactly("application-security.properties");
    }

}
