package com.educational.platform.dependency;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SpringFrameworkVersionCVETest {

    @Test
    void springCore_isAtLeastPatchedVersion() throws ReflectiveOperationException {
        // given
        String minimumVersion = "7.0.8";

        // when
        String actualVersion = springVersion();

        // then
        Assertions.assertNotNull(actualVersion, "SpringVersion.getVersion() must not be null");
        Assertions.assertTrue(isAtLeast(actualVersion, minimumVersion),
                () -> "Expected Spring Framework >= " + minimumVersion + " but was " + actualVersion);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "org.springframework.beans.factory.BeanFactory",
            "org.springframework.context.ApplicationContext",
            "org.springframework.aop.framework.ProxyFactory",
            "org.springframework.expression.ExpressionParser",
            "org.springframework.web.client.RestTemplate",
            "org.springframework.web.servlet.DispatcherServlet",
            "org.springframework.transaction.PlatformTransactionManager",
            "org.springframework.orm.jpa.JpaTransactionManager"
    })
    void springFrameworkModules_areAlignedOnSingleVersion(String className)
            throws ReflectiveOperationException {
        // given
        String expectedVersion = springVersion();

        // when
        Class<?> frameworkClass = Class.forName(className);
        String actualVersion = frameworkClass.getPackage().getImplementationVersion();

        // then
        Assertions.assertEquals(expectedVersion, actualVersion,
                () -> "Expected " + className + " to use Spring Framework " + expectedVersion
                        + " but was " + actualVersion);
    }

    @ParameterizedTest
    @CsvSource({
            "7.0.8, 7.0.8, true",
            "7.0.7, 7.0.8, false",
            "7.0.10, 7.0.8, true",
            "7.1.0, 7.0.8, true",
            "6.2.99, 7.0.8, false",
            "7.0.8-SNAPSHOT, 7.0.8, true"
    })
    void isAtLeast_comparesVersionComponentsNumerically(
            String actualVersion,
            String minimumVersion,
            boolean expected) {
        // given

        // when
        boolean result = isAtLeast(actualVersion, minimumVersion);

        // then
        Assertions.assertEquals(expected, result);
    }

    private static String springVersion() throws ReflectiveOperationException {
        return (String) Class.forName("org.springframework.core.SpringVersion")
                .getMethod("getVersion")
                .invoke(null);
    }

    private static boolean isAtLeast(String actualVersion, String minimumVersion) {
        int[] actualParts = parseVersion(actualVersion);
        int[] minimumParts = parseVersion(minimumVersion);

        for (int i = 0; i < actualParts.length; i++) {
            if (actualParts[i] != minimumParts[i]) {
                return actualParts[i] > minimumParts[i];
            }
        }
        return true;
    }

    private static int[] parseVersion(String version) {
        String[] parts = version.split("-", 2)[0].split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Expected major.minor.patch version: " + version);
        }
        return new int[] {
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
        };
    }
}
