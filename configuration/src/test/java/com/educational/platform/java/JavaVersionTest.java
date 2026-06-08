package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the project is compiled and running on Java 26.
 * <p>
 * These tests guard against misconfigured CI environments or build regressions
 * that would silently downgrade the target bytecode level.
 */
public class JavaVersionTest {

    private static final int JAVA_26_MAJOR_VERSION = 70;
    private static final int EXPECTED_JAVA_FEATURE_VERSION = 26;

    @Test
    void runtime_shouldBe_java26OrHigher() {
        int featureVersion = Runtime.version().feature();

        assertThat(featureVersion)
                .as("JVM runtime feature version must be >= 26")
                .isGreaterThanOrEqualTo(EXPECTED_JAVA_FEATURE_VERSION);
    }

    @Test
    void compiledClasses_shouldTarget_java26BytecodeVersion() throws IOException {
        // Read the class file major version of a known project class
        String classResource = "/com/educational/platform/java/JavaVersionTest.class";
        try (InputStream is = getClass().getResourceAsStream(classResource)) {
            assertThat(is)
                    .as("Class resource should be loadable: %s", classResource)
                    .isNotNull();

            DataInputStream dis = new DataInputStream(is);
            int magic = dis.readInt();
            assertThat(magic)
                    .as("Valid Java class file magic number")
                    .isEqualTo(0xCAFEBABE);

            dis.readUnsignedShort(); // minor version
            int majorVersion = dis.readUnsignedShort();

            assertThat(majorVersion)
                    .as("Class file major version should be %d (Java 26)", JAVA_26_MAJOR_VERSION)
                    .isEqualTo(JAVA_26_MAJOR_VERSION);
        }
    }

    @Test
    void runtimeVersion_specVersion_shouldBe26() {
        String specVersion = System.getProperty("java.specification.version");

        assertThat(specVersion)
                .as("java.specification.version should indicate Java 26")
                .isEqualTo("26");
    }

    @Test
    void compiledProductionClasses_shouldTarget_java26BytecodeVersion() throws IOException {
        String classResource = "/com/educational/platform/common/exception/ResourceNotFoundException.class";
        try (InputStream is = getClass().getResourceAsStream(classResource)) {
            assertThat(is)
                    .as("Production class resource should be loadable: %s", classResource)
                    .isNotNull();

            DataInputStream dis = new DataInputStream(is);
            int magic = dis.readInt();
            assertThat(magic)
                    .as("Valid Java class file magic number")
                    .isEqualTo(0xCAFEBABE);

            int minorVersion = dis.readUnsignedShort();
            int majorVersion = dis.readUnsignedShort();

            assertThat(majorVersion)
                    .as("Production class major version should be %d (Java 26)", JAVA_26_MAJOR_VERSION)
                    .isEqualTo(JAVA_26_MAJOR_VERSION);

            assertThat(minorVersion)
                    .as("Minor version should be 0 for standard release builds")
                    .isEqualTo(0);
        }
    }

    @Test
    void runtimeVersion_shouldHave_expectedVersionComponents() {
        Runtime.Version version = Runtime.version();

        assertThat(version.feature())
                .as("Feature version")
                .isEqualTo(EXPECTED_JAVA_FEATURE_VERSION);

        assertThat(version.interim())
                .as("Interim version should be 0 for GA releases")
                .isEqualTo(0);
    }

    @Test
    void runtime_classVersion_shouldMatch_java26() {
        String classVersion = System.getProperty("java.class.version");

        assertThat(classVersion)
                .as("java.class.version should be 70.0 for Java 26")
                .isEqualTo("70.0");
    }

    @Test
    void runtime_vmSpecVersion_shouldBe_26() {
        String vmSpecVersion = System.getProperty("java.vm.specification.version");

        assertThat(vmSpecVersion)
                .as("java.vm.specification.version should indicate Java 26")
                .isEqualTo("26");
    }

    @Test
    void javaClassFormatVersion_shouldBeConsistent_withRuntimeVersion() throws IOException {
        String classResource = "/com/educational/platform/java/JavaVersionTest.class";
        try (InputStream is = getClass().getResourceAsStream(classResource)) {
            assertThat(is).isNotNull();

            DataInputStream dis = new DataInputStream(is);
            dis.readInt(); // magic
            dis.readUnsignedShort(); // minor
            int majorVersion = dis.readUnsignedShort();

            // Class file major version = 44 + Java version
            int expectedMajor = 44 + Runtime.version().feature();
            assertThat(majorVersion)
                    .as("Class file major version should equal 44 + Java feature version")
                    .isEqualTo(expectedMajor);
        }
    }
}
