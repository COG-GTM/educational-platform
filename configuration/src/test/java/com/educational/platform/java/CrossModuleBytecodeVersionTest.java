package com.educational.platform.java;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that production classes from every bounded context are compiled
 * with the Java 26 class file major version (70).
 * <p>
 * A partial upgrade where one module still targets an older Java version
 * would not be caught by a single-class bytecode check. This test reads the
 * class file header of a representative class from each module.
 */
public class CrossModuleBytecodeVersionTest {

    private static final int JAVA_26_CLASS_FILE_MAJOR_VERSION = 70;

    static Stream<Arguments> moduleClasses() {
        return Stream.of(
                Arguments.of("courses",
                        "/com/educational/platform/courses/course/Course.class"),
                Arguments.of("administration",
                        "/com/educational/platform/administration/course/CourseProposal.class"),
                Arguments.of("course-enrollments",
                        "/com/educational/platform/course/enrollments/CourseEnrollment.class"),
                Arguments.of("course-reviews",
                        "/com/educational/platform/course/reviews/CourseReview.class"),
                Arguments.of("users",
                        "/com/educational/platform/users/User.class"),
                Arguments.of("common",
                        "/com/educational/platform/common/exception/ResourceNotFoundException.class")
        );
    }

    @ParameterizedTest(name = "{0} module class should be compiled with Java 26 bytecode")
    @MethodSource("moduleClasses")
    void productionClass_shouldHave_majorVersion70(String moduleName, String classResource) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(classResource)) {
            assertThat(is)
                    .as("Class resource from module '%s' should be on classpath: %s", moduleName, classResource)
                    .isNotNull();

            DataInputStream dis = new DataInputStream(is);
            int magic = dis.readInt();
            assertThat(magic)
                    .as("Valid Java class file magic number for %s module", moduleName)
                    .isEqualTo(0xCAFEBABE);

            int minorVersion = dis.readUnsignedShort();
            int majorVersion = dis.readUnsignedShort();

            assertThat(majorVersion)
                    .as("Class from '%s' module should have major version %d (Java 26)", moduleName, JAVA_26_CLASS_FILE_MAJOR_VERSION)
                    .isEqualTo(JAVA_26_CLASS_FILE_MAJOR_VERSION);

            assertThat(minorVersion)
                    .as("Minor version for '%s' module class should be 0 (GA release)", moduleName)
                    .isZero();
        }
    }

    @ParameterizedTest(name = "{0} module class should be loadable by reflection")
    @MethodSource("moduleClassNames")
    void productionClass_shouldBeLoadable_byReflection(String moduleName, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            assertThat(clazz).isNotNull();
            assertThat(clazz.getName()).isEqualTo(className);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Class from module '" + moduleName + "' should be loadable: " + className, e);
        }
    }

    static Stream<Arguments> moduleClassNames() {
        return Stream.of(
                Arguments.of("courses", "com.educational.platform.courses.course.Course"),
                Arguments.of("administration", "com.educational.platform.administration.course.CourseProposal"),
                Arguments.of("course-enrollments", "com.educational.platform.course.enrollments.CourseEnrollment"),
                Arguments.of("course-reviews", "com.educational.platform.course.reviews.CourseReview"),
                Arguments.of("users", "com.educational.platform.users.User"),
                Arguments.of("common", "com.educational.platform.common.exception.ResourceNotFoundException")
        );
    }
}
