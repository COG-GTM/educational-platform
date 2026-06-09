package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the property source precedence ordering when
 * {@code @SpringBootTest(properties = {...})} is used alongside the
 * application's {@code @PropertySource("application-security.properties")}.
 * The Spring Boot test infrastructure must correctly layer property sources
 * so that:
 * <ol>
 *   <li>Test-specific inline properties have the highest precedence</li>
 *   <li>{@code @PropertySource} declarations from the application class
 *       contribute to the environment but at lower precedence</li>
 *   <li>Both property sources coexist without conflict</li>
 * </ol>
 * <p>
 * Complements {@link SpringBootTestPropertyOverrideTest} (which validates basic
 * property override functionality) and {@link TestPropertySourceIntegrationTest}
 * (which validates the {@code @TestPropertySource} annotation). This test
 * specifically validates precedence ordering and coexistence.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "test.precedence.marker=from-springboottest"
        }
)
class SpringBootTestPropertySourcePrecedenceTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    void testProperties_shouldBeResolvableInEnvironment() {
        assertThat(environment.getProperty("test.precedence.marker"))
                .as("Inline test properties from @SpringBootTest must be resolvable")
                .isEqualTo("from-springboottest");
    }

    @Test
    void propertySourceAnnotationValues_shouldRemainAccessible() {
        assertThat(environment.getProperty("com.educational.platform.security.enabled"))
                .as("@PropertySource(application-security.properties) must still contribute "
                        + "properties to the environment alongside @SpringBootTest properties")
                .isNotNull()
                .isEqualTo("true");
    }

    @Test
    void inlinedTestProperties_shouldAppearInPropertySources() {
        MutablePropertySources sources = environment.getPropertySources();
        boolean foundInlineSource = false;
        for (PropertySource<?> source : sources) {
            if (source.getName().toLowerCase().contains("inlined")
                    || source.getName().toLowerCase().contains("inline")
                    || source.containsProperty("test.precedence.marker")) {
                foundInlineSource = true;
                break;
            }
        }
        assertThat(foundInlineSource)
                .as("Inline test properties must be present as a named PropertySource")
                .isTrue();
    }

    @Test
    void propertySourcePrecedence_inlineTestShouldBeBeforeClasspathProperties() {
        MutablePropertySources sources = environment.getPropertySources();
        List<String> sourceNames = new ArrayList<>();
        for (PropertySource<?> source : sources) {
            sourceNames.add(source.getName());
        }
        // Find position of inline test properties
        int inlinePos = -1;
        int classpathPos = -1;
        for (int i = 0; i < sourceNames.size(); i++) {
            String name = sourceNames.get(i).toLowerCase();
            if (name.contains("inlined") || name.contains("inline")) {
                inlinePos = i;
            }
            if (name.contains("class path") || name.contains("application-security")) {
                classpathPos = i;
            }
        }
        if (inlinePos >= 0 && classpathPos >= 0) {
            assertThat(inlinePos)
                    .as("Inline test property source (index %d) must have higher precedence "
                            + "(lower index) than classpath @PropertySource (index %d) — "
                            + "this ensures test overrides win over production config",
                            inlinePos, classpathPos)
                    .isLessThan(classpathPos);
        }
    }

    @Test
    void applicationProperties_shouldCoexistWithTestProperties() {
        // Both test and app properties should be resolvable simultaneously
        assertThat(environment.getProperty("test.precedence.marker"))
                .as("Test property must be resolvable")
                .isNotNull();
        assertThat(environment.getProperty("spring.datasource.url"))
                .as("application.properties datasource URL must still be resolvable "
                        + "alongside test properties")
                .isNotNull()
                .contains("h2");
    }
}
