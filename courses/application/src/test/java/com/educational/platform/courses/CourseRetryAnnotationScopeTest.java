package com.educational.platform.courses;

import com.educational.platform.courses.course.approve.ApproveCourseCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommand;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the <em>module-wide</em> scope of the retry feature: across the entire {@code courses}
 * production code, {@code @Retryable} must be applied to exactly the two course write paths the PR
 * annotated ({@link IncreaseNumberOfStudentsCommandHandler#handle} and
 * {@link UpdateCourseRatingCommandHandler#handle}) and to nothing else.
 *
 * <p>{@link CourseRetryScopeTest} and {@link CourseRetryProductionScopeContainmentTest} already
 * prove the {@code @EnableRetry} advisor stays off a sibling handler - but both pin a <em>single</em>
 * control, {@link ApproveCourseCommandHandler}. The module has several other {@code @Component
 * @Transactional} handlers that load -&gt; mutate -&gt; save a {@code Course}/{@code Teacher}
 * ({@code PublishCourseCommandHandler}, {@code SendCourseToApproveCommandHandler},
 * {@code CreateCourseCommandHandler}, {@code CreateTeacherCommandHandler}) plus the integration-event
 * handlers. A regression that added {@code @Retryable} to any of those - or hoisted it onto a shared
 * base type - would pass every existing test, because nothing enumerates the full set of annotated
 * methods. This test scans the whole production module and asserts the annotated set is exactly the
 * two intended methods, with no type-level annotation widening the contract.
 *
 * <p>Gradle compiles production sources to {@code .../build/classes/java/main} and test sources to
 * {@code .../build/classes/java/test}. Both are on the test runtime classpath and share the
 * {@code com.educational.platform.courses} package, so the scan filters by code-source location to
 * keep only production classes and exclude the test classes that live in the same package.
 */
class CourseRetryAnnotationScopeTest {

    private static final String COURSES_PRODUCTION_BASE_PACKAGE = "com.educational.platform.courses";
    private static final String TEST_OUTPUT_PATH_SEGMENT = "classes/java/test";

    @Test
    void coursesModule_appliesRetryableToExactlyTheTwoIntendedHandlerMethods() throws NoSuchMethodException {
        final Set<Method> retryableMethods = productionClasses().stream()
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(method -> AnnotatedElementUtils.hasAnnotation(method, Retryable.class))
                .collect(Collectors.toSet());

        // the only @Retryable methods in the whole courses module are the two course write paths
        assertThat(retryableMethods).containsExactlyInAnyOrder(
                IncreaseNumberOfStudentsCommandHandler.class.getMethod("handle", IncreaseNumberOfStudentsCommand.class),
                UpdateCourseRatingCommandHandler.class.getMethod("handle", UpdateCourseRatingCommand.class));
    }

    @Test
    void coursesModule_declaresNoTypeLevelRetryableAnnotation() {
        final List<Class<?>> typeAnnotated = productionClasses().stream()
                .filter(type -> AnnotatedElementUtils.hasAnnotation(type, Retryable.class))
                .collect(Collectors.toList());

        // @Retryable here is a method-level contract; a class-level (or shared-base) annotation would
        // silently widen retry onto every handle method of the annotated type, so none must exist
        assertThat(typeAnnotated).isEmpty();
    }

    @Test
    void scanResolvesTheCoursesProductionHandlers() {
        // guards the scan itself: a broken/empty scan would make the assertions above vacuously pass
        assertThat(productionClasses()).contains(
                IncreaseNumberOfStudentsCommandHandler.class,
                UpdateCourseRatingCommandHandler.class,
                ApproveCourseCommandHandler.class);
    }

    private static List<Class<?>> productionClasses() {
        final ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                        // consider every class in the package, not just @Component stereotypes
                        return true;
                    }
                };
        scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);

        return scanner.findCandidateComponents(COURSES_PRODUCTION_BASE_PACKAGE).stream()
                .map(BeanDefinition::getBeanClassName)
                .filter(Objects::nonNull)
                .filter(name -> !name.endsWith("package-info") && !name.endsWith("module-info"))
                .map(CourseRetryAnnotationScopeTest::loadClass)
                .filter(CourseRetryAnnotationScopeTest::isProductionClass)
                .collect(Collectors.toList());
    }

    private static Class<?> loadClass(String className) {
        try {
            // do not initialise: we only reflect over declared members and annotations
            return ClassUtils.forName(className, CourseRetryAnnotationScopeTest.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            throw new IllegalStateException("Failed to load scanned class " + className, e);
        }
    }

    private static boolean isProductionClass(Class<?> type) {
        final CodeSource codeSource = type.getProtectionDomain().getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            return false;
        }
        return !codeSource.getLocation().getPath().contains(TEST_OUTPUT_PATH_SEGMENT);
    }
}
