package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the {@code @ActiveProfiles} annotation works correctly
 * with {@code @SpringBootTest} to activate a specific Spring profile.
 * {@link ActiveProfileConfigurationTest} validates profile activation
 * programmatically via {@code SpringApplication.run()}; this test validates
 * the annotation-based approach that developers use in test classes. The
 * annotation path goes through the Spring TestContext Framework's profile
 * resolution, which is a different code path than programmatic activation.
 * <p>
 * Without spring-boot-starter-test, the {@code @ActiveProfiles} integration
 * with {@code @SpringBootTest} would not work because the
 * {@code SpringBootTestContextBootstrapper} — which bridges Spring Boot's
 * context creation with the TestContext Framework — would not be on the
 * classpath.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
class SpringBootTestActiveProfilesIntegrationTest {

    @Autowired
    private Environment environment;

    @Test
    void testProfile_shouldBeActive() {
        assertThat(environment.getActiveProfiles())
                .as("@ActiveProfiles(\"test\") must activate the 'test' profile in the "
                        + "Spring Environment when used with @SpringBootTest")
                .contains("test");
    }

    @Test
    void defaultProfile_shouldNotBeActive_whenExplicitProfileIsSet() {
        assertThat(environment.getActiveProfiles())
                .as("When an explicit profile is activated, it must replace the default profile")
                .doesNotContain("default");
    }

    @Test
    void applicationProperties_shouldStillBeLoaded() {
        assertThat(environment.getProperty("spring.datasource.url"))
                .as("application.properties must still be loaded regardless of active profile — "
                        + "base properties are always included in Spring Boot's property resolution")
                .isNotNull();
    }

    @Test
    void securityPropertySource_shouldStillBeLoaded() {
        assertThat(environment.containsProperty("com.educational.platform.security.enabled"))
                .as("@PropertySource(application-security.properties) must be loaded even when "
                        + "@ActiveProfiles is used — @PropertySource is independent of profile activation")
                .isTrue();
    }

    @Test
    void environment_shouldAcceptProfileFromAnnotation() {
        assertThat(environment.acceptsProfiles(
                org.springframework.core.env.Profiles.of("test")))
                .as("Environment.acceptsProfiles must return true for the profile "
                        + "activated via @ActiveProfiles")
                .isTrue();
    }
}
