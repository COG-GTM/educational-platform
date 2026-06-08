package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that classes compiled from different modules all target the same
 * Java 26 bytecode version (class file major version 70).
 * <p>
 * The root {@code build.gradle.kts} sets {@code sourceCompatibility} and
 * {@code targetCompatibility} to {@code VERSION_26} for all subprojects.
 * A misconfigured submodule could silently compile to a different target.
 * <p>
 * {@link JavaVersionTest} validates the test module itself.
 * This test validates classes from <em>multiple bounded contexts</em> to
 * ensure the root-level setting propagates correctly to every module.
 */
public class BytecodeMajorVersionCrossModuleTest {

    private static final int JAVA_26_CLASS_FILE_MAJOR = 70;

    @ParameterizedTest(name = "Class {0} should have bytecode major version 70")
    @ValueSource(strings = {
            // configuration module (entry point)
            "/com/educational/platform/java/BytecodeMajorVersionCrossModuleTest.class",
            // common module
            "/com/educational/platform/common/exception/ResourceNotFoundException.class",
    })
    void classFromModule_shouldHave_java26BytecodeVersion(String classResource) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(classResource)) {
            assertThat(is)
                    .as("Class resource should be loadable: %s", classResource)
                    .isNotNull();

            DataInputStream dis = new DataInputStream(is);
            int magic = dis.readInt();
            assertThat(magic)
                    .as("Valid Java class file magic number for %s", classResource)
                    .isEqualTo(0xCAFEBABE);

            int minorVersion = dis.readUnsignedShort();
            int majorVersion = dis.readUnsignedShort();

            assertThat(majorVersion)
                    .as("Class file major version for %s should be %d (Java 26)",
                            classResource, JAVA_26_CLASS_FILE_MAJOR)
                    .isEqualTo(JAVA_26_CLASS_FILE_MAJOR);

            assertThat(minorVersion)
                    .as("Minor version for %s should be 0 (release build)", classResource)
                    .isEqualTo(0);
        }
    }

    @Test
    void classFileMajorVersion_formula_shouldHold() {
        int runtimeFeature = Runtime.version().feature();
        int expectedMajor = 44 + runtimeFeature;

        assertThat(expectedMajor)
                .as("44 + Java %d should equal %d", runtimeFeature, JAVA_26_CLASS_FILE_MAJOR)
                .isEqualTo(JAVA_26_CLASS_FILE_MAJOR);
    }

    @Test
    void classFileVersion_asSystemProperty_shouldBe_70() {
        String classVersion = System.getProperty("java.class.version");

        assertThat(classVersion)
                .as("java.class.version system property should be 70.0 for Java 26")
                .isEqualTo("70.0");
    }

    @Test
    void testClass_bytecode_shouldEqual_productionClass_bytecodeVersion() throws IOException {
        int testMajor = readMajorVersion(
                "/com/educational/platform/java/BytecodeMajorVersionCrossModuleTest.class");
        int prodMajor = readMajorVersion(
                "/com/educational/platform/common/exception/ResourceNotFoundException.class");

        assertThat(testMajor)
                .as("Test class bytecode version should equal production class bytecode version")
                .isEqualTo(prodMajor);
    }

    private int readMajorVersion(String classResource) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(classResource)) {
            assertThat(is).as("Should load %s", classResource).isNotNull();
            DataInputStream dis = new DataInputStream(is);
            dis.readInt();              // magic
            dis.readUnsignedShort();    // minor
            return dis.readUnsignedShort(); // major
        }
    }
}
