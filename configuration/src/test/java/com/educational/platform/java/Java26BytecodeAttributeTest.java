package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Extended bytecode attribute verification for Java 26 compiled classes.
 * <p>
 * Goes beyond the major version check in {@link CrossModuleBytecodeVersionTest}
 * by validating:
 * <ul>
 *   <li>Minor version is 0 (GA release, not preview-enabled)</li>
 *   <li>Class file magic number is correct for all loaded classes</li>
 *   <li>Record classes compile with proper bytecode structure</li>
 *   <li>Sealed classes/interfaces are loadable and have correct modifiers</li>
 *   <li>The test class itself is valid Java 26 bytecode</li>
 * </ul>
 */
public class Java26BytecodeAttributeTest {

    private static final int JAVA_26_MAJOR_VERSION = 70;
    private static final int EXPECTED_MINOR_VERSION = 0;
    private static final int CLASS_FILE_MAGIC = 0xCAFEBABE;

    // --- Minor version validation (ensures no --enable-preview) ---

    @ParameterizedTest(name = "Class {0} should have minor version 0 (no preview)")
    @MethodSource("productionClassResources")
    void productionClass_shouldHave_minorVersionZero(String description, String classResource)
            throws IOException {
        try (InputStream is = getClass().getResourceAsStream(classResource)) {
            assertThat(is)
                    .as("Class resource should exist: %s", classResource)
                    .isNotNull();

            DataInputStream dis = new DataInputStream(is);
            dis.readInt(); // magic
            int minorVersion = dis.readUnsignedShort();

            assertThat(minorVersion)
                    .as("Minor version for %s should be 0 (GA, no preview features)", description)
                    .isEqualTo(EXPECTED_MINOR_VERSION);
        }
    }

    static Stream<Arguments> productionClassResources() {
        return Stream.of(
                Arguments.of("Course entity",
                        "/com/educational/platform/courses/course/Course.class"),
                Arguments.of("CourseProposal entity",
                        "/com/educational/platform/administration/course/CourseProposal.class"),
                Arguments.of("User entity",
                        "/com/educational/platform/users/User.class"),
                Arguments.of("CourseEnrollment entity",
                        "/com/educational/platform/course/enrollments/CourseEnrollment.class"),
                Arguments.of("ResourceNotFoundException",
                        "/com/educational/platform/common/exception/ResourceNotFoundException.class")
        );
    }

    // --- Magic number validation ---

    @ParameterizedTest(name = "Class {0} should have valid magic number 0xCAFEBABE")
    @MethodSource("productionClassResources")
    void productionClass_shouldHave_validMagicNumber(String description, String classResource)
            throws IOException {
        try (InputStream is = getClass().getResourceAsStream(classResource)) {
            assertThat(is).isNotNull();

            DataInputStream dis = new DataInputStream(is);
            int magic = dis.readInt();

            assertThat(magic)
                    .as("Magic number for %s", description)
                    .isEqualTo(CLASS_FILE_MAGIC);
        }
    }

    // --- Test class self-validation ---

    @Test
    void thisTestClass_shouldBe_compiledWithJava26() throws IOException {
        String resourcePath = "/" + getClass().getName().replace('.', '/') + ".class";
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            assertThat(is).as("Test class resource should be accessible").isNotNull();

            DataInputStream dis = new DataInputStream(is);
            int magic = dis.readInt();
            int minor = dis.readUnsignedShort();
            int major = dis.readUnsignedShort();

            assertThat(magic).isEqualTo(CLASS_FILE_MAGIC);
            assertThat(major)
                    .as("This test class major version should be 70 (Java 26)")
                    .isEqualTo(JAVA_26_MAJOR_VERSION);
            assertThat(minor)
                    .as("This test class minor version should be 0")
                    .isEqualTo(EXPECTED_MINOR_VERSION);
        }
    }

    // --- Record class bytecode ---

    @Test
    void recordClass_shouldBeLoadable_andMarkedAsRecord() {
        // Records (Java 16+) have the ACC_RECORD attribute in bytecode
        record TestRecord(String name, int value) {}

        assertThat(TestRecord.class.isRecord())
                .as("Record class should report isRecord() == true on Java 26")
                .isTrue();

        assertThat(TestRecord.class.getRecordComponents())
                .as("Record should have 2 components")
                .hasSize(2);
    }

    @Test
    void recordClass_bytecodeVersion_shouldBeJava26() throws IOException {
        // Define and check a record compiled in this test
        record VersionRecord(int major, int minor, int patch) {}

        String resourcePath = "/" + VersionRecord.class.getName().replace('.', '/') + ".class";
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            assertThat(is).as("Record class resource should exist").isNotNull();

            DataInputStream dis = new DataInputStream(is);
            int magic = dis.readInt();
            dis.readUnsignedShort(); // minor
            int major = dis.readUnsignedShort();

            assertThat(magic).isEqualTo(CLASS_FILE_MAGIC);
            assertThat(major)
                    .as("Record compiled at Java 26 should have major version 70")
                    .isEqualTo(JAVA_26_MAJOR_VERSION);
        }
    }

    // --- Sealed class bytecode ---

    sealed interface UpgradeStatus permits UpgradeStatus.Success, UpgradeStatus.Failure {}
    record Success(String version) implements UpgradeStatus {}
    record Failure(String reason) implements UpgradeStatus {}

    @Test
    void sealedInterface_shouldBeLoadable_onJava26() {
        assertThat(UpgradeStatus.class.isSealed())
                .as("Sealed interface should report isSealed() == true")
                .isTrue();

        assertThat(UpgradeStatus.class.getPermittedSubclasses())
                .as("Sealed interface should have 2 permitted subclasses")
                .hasSize(2);
    }

    @Test
    void sealedInterface_permittedSubclasses_shouldBeAccessible() {
        Class<?>[] permitted = UpgradeStatus.class.getPermittedSubclasses();

        assertThat(permitted)
                .extracting(Class::getSimpleName)
                .containsExactlyInAnyOrder("Success", "Failure");
    }

    // --- Class loading should not throw verification errors ---

    @ParameterizedTest(name = "Domain class {0} should pass JVM bytecode verification")
    @MethodSource("domainClasses")
    void domainClass_shouldPassBytecodeVerification(String className) {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(className);
            // Trigger class initialization to ensure bytecode verification passes
            assertThat(clazz.getDeclaredFields()).isNotNull();
            assertThat(clazz.getDeclaredMethods()).isNotNull();
            assertThat(clazz.getDeclaredConstructors()).isNotNull();
        }).as("Class %s should load and verify without errors on Java 26", className)
                .doesNotThrowAnyException();
    }

    static Stream<Arguments> domainClasses() {
        return Stream.of(
                Arguments.of("com.educational.platform.courses.course.Course"),
                Arguments.of("com.educational.platform.administration.course.CourseProposal"),
                Arguments.of("com.educational.platform.course.enrollments.CourseEnrollment"),
                Arguments.of("com.educational.platform.course.reviews.CourseReview"),
                Arguments.of("com.educational.platform.users.User"),
                Arguments.of("com.educational.platform.common.exception.ResourceNotFoundException")
        );
    }

    // --- Reflection access sanity check ---

    @Test
    void reflectionAccess_shouldWork_onJava26CompiledClasses() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");

        assertThat(courseClass.getDeclaredFields().length)
                .as("Course should have multiple declared fields")
                .isGreaterThan(0);

        assertThat(courseClass.getSuperclass())
                .as("Course superclass should be accessible")
                .isNotNull();

        assertThat(courseClass.getInterfaces())
                .as("Course interfaces array should be accessible (even if empty)")
                .isNotNull();
    }
}
