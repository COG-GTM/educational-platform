package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the proxyBeanMethods attribute of @SpringBootApplication.
 * This attribute controls whether @Bean methods are proxied via CGLIB.
 * Changing it to false (lite mode) would break inter-@Bean dependencies
 * where one @Bean method calls another, and could subtly affect
 * the auto-configuration behavior enabled by the Spring Boot plugin.
 */
class SpringBootApplicationProxyBeanMethodsTest {

    @Test
    void springBootApplication_proxyBeanMethods_shouldBeTrue() {
        SpringBootApplication sba = EducationalPlatformApplication.class
                .getAnnotation(SpringBootApplication.class);
        assertThat(sba).isNotNull();
        assertThat(sba.proxyBeanMethods())
                .as("proxyBeanMethods must be true (default full mode) to ensure "
                        + "inter-@Bean method calls return singleton instances")
                .isTrue();
    }

    @Test
    void springBootApplication_shouldCarryConfigurationAnnotation() {
        SpringBootApplication sba = EducationalPlatformApplication.class
                .getAnnotation(SpringBootApplication.class);
        assertThat(sba).isNotNull();

        boolean hasConfiguration = sba.annotationType()
                .isAnnotationPresent(Configuration.class)
                || java.util.Arrays.stream(sba.annotationType().getAnnotations())
                .anyMatch(a -> {
                    // @SpringBootConfiguration carries @Configuration
                    return a.annotationType().isAnnotationPresent(Configuration.class);
                });
        assertThat(hasConfiguration)
                .as("@SpringBootApplication must transitively carry @Configuration "
                        + "for the application class to act as a configuration source")
                .isTrue();
    }

    @Test
    void springBootApplication_annotationType_shouldNotOverrideDefaultAnnotation() {
        SpringBootApplication sba = EducationalPlatformApplication.class
                .getAnnotation(SpringBootApplication.class);
        assertThat(sba).isNotNull();
        assertThat(sba.annotationType().getAnnotation(Configuration.class))
                .as("@Configuration must be accessible from the @SpringBootApplication meta-annotation chain")
                .isNull(); // @Configuration is on @SpringBootConfiguration, not directly on @SpringBootApplication

        // Verify the chain: @SpringBootApplication -> @SpringBootConfiguration -> @Configuration
        var springBootConfig = sba.annotationType()
                .getAnnotation(org.springframework.boot.SpringBootConfiguration.class);
        assertThat(springBootConfig).isNotNull();
        assertThat(springBootConfig.annotationType().isAnnotationPresent(Configuration.class))
                .as("@SpringBootConfiguration must carry @Configuration")
                .isTrue();
    }
}
