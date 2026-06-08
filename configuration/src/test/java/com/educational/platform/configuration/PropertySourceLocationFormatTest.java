package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the format of the @PropertySource location value on the
 * application entry point. The annotation uses a bare filename
 * ("application-security.properties") without a classpath: prefix,
 * relying on Spring's default resource resolution. Changing this
 * format (e.g., to "classpath:..." or "file:...") could alter
 * resolution behavior with the Spring Boot plugin's fat JAR classloader,
 * where classpath resources are nested inside BOOT-INF/classes/.
 * <p>
 * Complements {@link PropertySourceAttributeValidationTest} (attribute
 * defaults) and {@link ConfigurationModuleResourceValidationTest}
 * (file existence on classpath).
 */
class PropertySourceLocationFormatTest {

    @Test
    void propertySourceLocation_shouldNotUseClasspathPrefix() {
        PropertySource ps = EducationalPlatformApplication.class.getAnnotation(PropertySource.class);
        assertThat(ps).isNotNull();
        for (String location : ps.value()) {
            assertThat(location)
                    .as("@PropertySource location must use bare filename without classpath: prefix — "
                            + "Spring resolves bare names from the classpath by default, and the "
                            + "explicit prefix is unnecessary and could cause issues with "
                            + "the Spring Boot fat JAR classloader")
                    .doesNotStartWith("classpath:");
        }
    }

    @Test
    void propertySourceLocation_shouldNotUseFilePrefix() {
        PropertySource ps = EducationalPlatformApplication.class.getAnnotation(PropertySource.class);
        assertThat(ps).isNotNull();
        for (String location : ps.value()) {
            assertThat(location)
                    .as("@PropertySource location must not use file: prefix — "
                            + "the security properties must be loaded from the classpath (inside the JAR), "
                            + "not from an external filesystem path")
                    .doesNotStartWith("file:");
        }
    }

    @Test
    void propertySourceLocation_shouldEndWithPropertiesExtension() {
        PropertySource ps = EducationalPlatformApplication.class.getAnnotation(PropertySource.class);
        assertThat(ps).isNotNull();
        for (String location : ps.value()) {
            assertThat(location)
                    .as("@PropertySource location must reference a .properties file — "
                            + "YAML is not supported by @PropertySource without a custom factory")
                    .endsWith(".properties");
        }
    }

    @Test
    void propertySourceLocation_shouldNotContainPathSeparators() {
        PropertySource ps = EducationalPlatformApplication.class.getAnnotation(PropertySource.class);
        assertThat(ps).isNotNull();
        for (String location : ps.value()) {
            assertThat(location)
                    .as("@PropertySource location must be a flat filename at the classpath root — "
                            + "subdirectory paths would require the file to be in a non-standard location")
                    .doesNotContain("/")
                    .doesNotContain("\\");
        }
    }

    @Test
    void propertySourceLocation_shouldFollowSpringBootNamingConvention() {
        PropertySource ps = EducationalPlatformApplication.class.getAnnotation(PropertySource.class);
        assertThat(ps).isNotNull();
        assertThat(ps.value()[0])
                .as("Security properties file must follow the application-{profile}.properties convention")
                .startsWith("application-");
    }
}
