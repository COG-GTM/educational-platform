package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that JVM system properties consistently report Java 26
 * after the upgrade from Java 25.
 * <p>
 * {@link JavaVersionTest} checks {@code Runtime.version().feature()} and
 * the class file major version of a compiled class. This test complements
 * it by verifying the <em>system properties</em> that frameworks and build
 * tools read at runtime to make version-dependent decisions
 * ({@code java.specification.version}, {@code java.class.version},
 * {@code java.vm.specification.version}). A mismatch between these
 * properties and the actual runtime feature version can cause tools like
 * Spring Boot, Hibernate, or ArchUnit to apply incorrect code paths.
 */
public class Java26SystemPropertiesTest {

    private static final int EXPECTED_FEATURE_VERSION = 26;
    private static final int EXPECTED_CLASS_FILE_MAJOR = 70; // 44 + 26

    @Test
    void javaSpecificationVersion_shouldBe_26() {
        String specVersion = System.getProperty("java.specification.version");

        assertThat(specVersion)
                .as("java.specification.version should reflect Java 26")
                .isNotNull()
                .isEqualTo(String.valueOf(EXPECTED_FEATURE_VERSION));
    }

    @Test
    void javaVmSpecificationVersion_shouldBe_26() {
        String vmSpecVersion = System.getProperty("java.vm.specification.version");

        assertThat(vmSpecVersion)
                .as("java.vm.specification.version should reflect Java 26")
                .isNotNull()
                .isEqualTo(String.valueOf(EXPECTED_FEATURE_VERSION));
    }

    @Test
    void javaClassVersion_majorComponent_shouldBe_70() {
        String classVersion = System.getProperty("java.class.version");

        assertThat(classVersion)
                .as("java.class.version should be present")
                .isNotNull();

        int majorVersion = (int) Double.parseDouble(classVersion);
        assertThat(majorVersion)
                .as("java.class.version major component should be %d (44 + %d)",
                        EXPECTED_CLASS_FILE_MAJOR, EXPECTED_FEATURE_VERSION)
                .isEqualTo(EXPECTED_CLASS_FILE_MAJOR);
    }

    @Test
    void runtimeFeatureVersion_shouldMatch_specificationProperty() {
        int runtimeFeature = Runtime.version().feature();
        String specVersion = System.getProperty("java.specification.version");

        assertThat(runtimeFeature)
                .as("Runtime.version().feature() should match java.specification.version")
                .isEqualTo(Integer.parseInt(specVersion));
    }

    @Test
    void javaVersion_shouldStartWith_26() {
        String javaVersion = System.getProperty("java.version");

        assertThat(javaVersion)
                .as("java.version should start with '26'")
                .isNotNull()
                .startsWith("26");
    }

    @Test
    void javaVersionShouldNotReferenceOld25() {
        String javaVersion = System.getProperty("java.version");

        assertThat(javaVersion)
                .as("java.version should not reference old version 25")
                .doesNotStartWith("25");
    }

    @ParameterizedTest(name = "System property ''{0}'' should be non-null")
    @CsvSource({
            "java.version",
            "java.specification.version",
            "java.vm.specification.version",
            "java.class.version",
            "java.runtime.version",
            "java.vendor",
            "java.home"
    })
    void javaSystemProperty_shouldBePresent(String propertyName) {
        assertThat(System.getProperty(propertyName))
                .as("System property '%s' should be non-null", propertyName)
                .isNotNull()
                .isNotBlank();
    }

    @Test
    void allVersionProperties_shouldBeCoherent() {
        int specVersion = Integer.parseInt(System.getProperty("java.specification.version"));
        int vmSpecVersion = Integer.parseInt(System.getProperty("java.vm.specification.version"));
        int classFileMajor = (int) Double.parseDouble(System.getProperty("java.class.version"));
        int runtimeFeature = Runtime.version().feature();

        assertThat(specVersion)
                .as("All version indicators should agree")
                .isEqualTo(vmSpecVersion)
                .isEqualTo(runtimeFeature)
                .isEqualTo(classFileMajor - 44);
    }
}
