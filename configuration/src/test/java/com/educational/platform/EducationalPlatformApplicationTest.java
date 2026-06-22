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
    void applicationClass_hasMainMethod() throws NoSuchMethodException {
        java.lang.reflect.Method main = EducationalPlatformApplication.class.getMethod("main", String[].class);
        assertThat(main).isNotNull();
        assertThat(java.lang.reflect.Modifier.isStatic(main.getModifiers())).isTrue();
        assertThat(java.lang.reflect.Modifier.isPublic(main.getModifiers())).isTrue();
        assertThat(main.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void applicationClass_isPublic() {
        assertThat(java.lang.reflect.Modifier.isPublic(EducationalPlatformApplication.class.getModifiers())).isTrue();
    }
}
