package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates the JVM runtime environment is correctly configured for Java 26.
 * <p>
 * These tests verify system properties, module system accessibility, and
 * runtime capabilities that must be present for the application to function
 * correctly under Java 26. They guard against running on an older JVM that
 * does not match the build target.
 */
public class Java26RuntimeEnvironmentTest {

    @Test
    void jvmVendor_shouldBeRecognized() {
        String vendor = System.getProperty("java.vendor");

        assertThat(vendor)
                .as("JVM vendor should be set")
                .isNotBlank();
    }

    @Test
    void jvmHome_shouldBeSet() {
        String javaHome = System.getProperty("java.home");

        assertThat(javaHome)
                .as("java.home should be configured")
                .isNotBlank();
    }

    @Test
    void runtimeVersion_shouldBe_26OrHigher() {
        Runtime.Version version = Runtime.version();

        assertThat(version.feature())
                .as("Runtime feature version should be >= 26")
                .isGreaterThanOrEqualTo(26);

        assertThat(version.toString())
                .as("Full runtime version string")
                .startsWith("26");
    }

    @Test
    void classFileVersion_majorFormula_shouldHold() {
        // Class file major version = 44 + Java feature version
        int expectedMajor = 44 + Runtime.version().feature();

        assertThat(expectedMajor)
                .as("Major version formula: 44 + %d = %d", Runtime.version().feature(), expectedMajor)
                .isEqualTo(70);
    }

    @ParameterizedTest(name = "System property ''{0}'' should be set")
    @ValueSource(strings = {
            "java.version",
            "java.specification.version",
            "java.vm.specification.version",
            "java.class.version",
            "java.vendor",
            "java.home",
            "java.class.path"
    })
    void systemProperty_shouldBeSet(String propertyName) {
        String value = System.getProperty(propertyName);

        assertThat(value)
                .as("System property '%s' should be set", propertyName)
                .isNotNull()
                .isNotBlank();
    }

    @Test
    void moduleSystem_shouldBeAccessible() {
        Module module = getClass().getModule();

        assertThat(module)
                .as("Current class should have a Module reference (Java 9+ module system)")
                .isNotNull();
    }

    @Test
    void javaBaseModule_shouldBePresent() {
        Module javaBase = Object.class.getModule();

        assertThat(javaBase.getName())
                .as("Object class should be in java.base module")
                .isEqualTo("java.base");
    }

    @Test
    void availableProcessors_shouldBePositive() {
        int processors = Runtime.getRuntime().availableProcessors();

        assertThat(processors)
                .as("Available processors should be at least 1")
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void maxMemory_shouldBePositive() {
        long maxMemory = Runtime.getRuntime().maxMemory();

        assertThat(maxMemory)
                .as("Max memory should be positive")
                .isGreaterThan(0);
    }

    @Test
    void virtualThreads_shouldBeAvailable() {
        // Virtual threads became a final feature in Java 21
        assertThatCode(() -> {
            Thread vThread = Thread.ofVirtual().unstarted(() -> {});
            assertThat(vThread.isVirtual()).isTrue();
        }).as("Virtual threads should be available on Java 26")
                .doesNotThrowAnyException();
    }

    @Test
    void structuredTaskScope_classAvailable() {
        // StructuredTaskScope finalized in Java 24 (JEP 480)
        assertThatCode(() ->
                Class.forName("java.util.concurrent.StructuredTaskScope")
        ).as("StructuredTaskScope should be available on Java 26 (finalized in Java 24)")
                .doesNotThrowAnyException();
    }

    @Test
    void scopedValues_classAvailable() {
        // ScopedValue finalized in Java 25 (JEP 487)
        assertThatCode(() ->
                Class.forName("java.lang.ScopedValue")
        ).as("ScopedValue should be available on Java 26 (finalized in Java 25)")
                .doesNotThrowAnyException();
    }

    @Test
    void runtime_version_shouldHave_correctUpdateVersion() {
        Runtime.Version version = Runtime.version();

        assertThat(version.update())
                .as("Update version should be non-negative")
                .isGreaterThanOrEqualTo(0);
    }
}
