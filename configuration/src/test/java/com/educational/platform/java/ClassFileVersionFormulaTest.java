package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the class file major version formula: {@code major = 44 + javaVersion}.
 * <p>
 * This formula is used by several upgrade tests to assert bytecode compatibility.
 * If the formula were wrong, those tests would either false-pass or false-fail.
 * This test cross-validates the formula against historically known Java-to-major
 * version mappings and the actual runtime, ensuring the foundation of our
 * bytecode verification tests is correct.
 */
public class ClassFileVersionFormulaTest {

    private static final int CLASS_FILE_VERSION_OFFSET = 44;

    @ParameterizedTest(name = "Java {0} should produce class file major version {1}")
    @CsvSource({
            " 1,  45",
            " 2,  46",
            " 5,  49",
            " 6,  50",
            " 7,  51",
            " 8,  52",
            " 9,  53",
            "10,  54",
            "11,  55",
            "12,  56",
            "14,  58",
            "16,  60",
            "17,  61",
            "21,  65",
            "22,  66",
            "23,  67",
            "24,  68",
            "25,  69",
            "26,  70"
    })
    void formula_shouldMap_javaVersionToMajorVersion(int javaVersion, int expectedMajor) {
        int computedMajor = CLASS_FILE_VERSION_OFFSET + javaVersion;

        assertThat(computedMajor)
                .as("44 + %d should equal %d", javaVersion, expectedMajor)
                .isEqualTo(expectedMajor);
    }

    @Test
    void runtimeVersion_shouldMatchFormula() {
        int runtimeFeature = Runtime.version().feature();
        int expectedMajor = CLASS_FILE_VERSION_OFFSET + runtimeFeature;

        String classVersion = System.getProperty("java.class.version");
        int actualMajor = (int) Double.parseDouble(classVersion);

        assertThat(actualMajor)
                .as("java.class.version major (%s) should equal 44 + %d = %d",
                        classVersion, runtimeFeature, expectedMajor)
                .isEqualTo(expectedMajor);
    }

    @Test
    void compiledTestClass_majorVersion_shouldMatchFormula() throws IOException {
        String classResource = "/" + getClass().getName().replace('.', '/') + ".class";
        try (InputStream is = getClass().getResourceAsStream(classResource)) {
            assertThat(is).as("Test class resource should exist").isNotNull();

            DataInputStream dis = new DataInputStream(is);
            int magic = dis.readInt();
            assertThat(magic).isEqualTo(0xCAFEBABE);

            dis.readUnsignedShort(); // minor
            int majorVersion = dis.readUnsignedShort();

            int expectedMajor = CLASS_FILE_VERSION_OFFSET + Runtime.version().feature();
            assertThat(majorVersion)
                    .as("Compiled test class major version should equal 44 + %d", Runtime.version().feature())
                    .isEqualTo(expectedMajor);
        }
    }

    @Test
    void java26MajorVersion_shouldBe70() {
        assertThat(CLASS_FILE_VERSION_OFFSET + 26)
                .as("Java 26 class file major version")
                .isEqualTo(70);
    }

    @ParameterizedTest(name = "Inverse: major version {0} should map back to Java {1}")
    @CsvSource({
            "52,  8",
            "55, 11",
            "61, 17",
            "65, 21",
            "69, 25",
            "70, 26"
    })
    void inverseFormula_shouldMap_majorVersionToJavaVersion(int majorVersion, int expectedJava) {
        int computedJava = majorVersion - CLASS_FILE_VERSION_OFFSET;

        assertThat(computedJava)
                .as("Major version %d - 44 should equal Java %d", majorVersion, expectedJava)
                .isEqualTo(expectedJava);
    }
}
