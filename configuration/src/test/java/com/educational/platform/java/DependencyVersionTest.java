package com.educational.platform.java;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies that key dependencies are at versions compatible with Java 26.
 * <p>
 * ArchUnit < 1.4.2 cannot parse class file major version 70 (Java 26).
 * This test catches accidental downgrades of critical dependencies.
 */
public class DependencyVersionTest {

    @Test
    void archUnit_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("com.tngtech.archunit.core.importer.ClassFileImporter"))
                .as("ArchUnit ClassFileImporter should be loadable from classpath")
                .doesNotThrowAnyException();
    }

    @Test
    void archUnit_shouldHandle_classFileMajorVersion70() {
        // Java 26 = class file major version 70
        // ArchUnit < 1.4.2 throws "Unsupported class file major version 70"
        assertThatCode(() -> new ClassFileImporter().importClasses(DependencyVersionTest.class))
                .as("ArchUnit should handle major version 70 without throwing")
                .doesNotThrowAnyException();
    }

    @Test
    void junitJupiter_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.junit.jupiter.api.Test"))
                .as("JUnit Jupiter API should be loadable from classpath")
                .doesNotThrowAnyException();
    }

    @Test
    void assertJ_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.assertj.core.api.Assertions"))
                .as("AssertJ Core should be loadable from classpath")
                .doesNotThrowAnyException();
    }

    @Test
    void mockito_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.mockito.Mockito"))
                .as("Mockito should be loadable from classpath")
                .doesNotThrowAnyException();
    }
}
