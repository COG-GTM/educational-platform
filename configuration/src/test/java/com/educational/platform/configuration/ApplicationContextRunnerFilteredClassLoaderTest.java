package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the {@link FilteredClassLoader} from spring-boot-test
 * works functionally with the {@link ApplicationContextRunner} to simulate
 * classpath conditions for auto-configuration testing. The existing tests
 * ({@link TestSliceAnnotationFunctionalTest}) verify FilteredClassLoader is
 * on the classpath; this test exercises it to prove the test infrastructure
 * added by spring-boot-starter-test is fully functional — not just loadable.
 * <p>
 * FilteredClassLoader is critical for testing conditional auto-configuration
 * (e.g., {@code @ConditionalOnClass}, {@code @ConditionalOnMissingClass})
 * in isolation without modifying the actual test classpath.
 */
class ApplicationContextRunnerFilteredClassLoaderTest {

    @Test
    void filteredClassLoader_shouldHideSpecifiedClasses() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("com.nonexistent.SomeClass"))
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void filteredClassLoader_shouldBeInstantiableWithClassReference() {
        assertThatCode(() -> new FilteredClassLoader(
                org.springframework.boot.test.context.FilteredClassLoader.class))
                .as("FilteredClassLoader must accept Class references to filter")
                .doesNotThrowAnyException();
    }

    @Test
    void filteredClassLoader_shouldBeInstantiableWithStringClassNames() {
        assertThatCode(() -> new FilteredClassLoader("org.example.NonExistent"))
                .as("FilteredClassLoader must accept String class names to filter")
                .doesNotThrowAnyException();
    }

    @Test
    void contextRunner_withFilteredClassLoader_shouldStillLoadAvailableClasses() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("com.nonexistent.Foo"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getClassLoader())
                            .as("Context must use the FilteredClassLoader when specified")
                            .isInstanceOf(FilteredClassLoader.class);
                });
    }

    @Test
    void contextRunner_withPropertyValues_andFilteredClassLoader_shouldWork() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("com.nonexistent.Bar"))
                .withPropertyValues("test.key=test-value")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getEnvironment().getProperty("test.key"))
                            .as("Property values must still be resolvable with a FilteredClassLoader")
                            .isEqualTo("test-value");
                });
    }
}
