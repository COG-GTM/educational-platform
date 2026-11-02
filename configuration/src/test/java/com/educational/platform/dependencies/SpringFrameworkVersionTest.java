package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopProxy;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.SpringVersion;
import org.springframework.expression.ExpressionParser;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against regressing to a Spring Framework release affected by CVE-2026-41852
 * (SNYK-JAVA-ORGSPRINGFRAMEWORK-17253570, fixed in spring-expression 7.0.8).
 */
class SpringFrameworkVersionTest {

    private static final int[] MINIMUM = {7, 0, 8};

    @Test
    void springExpressionIsPatched() {
        assertPatched(ExpressionParser.class.getPackage().getImplementationVersion());
    }

    @Test
    void springCoreIsPatched() {
        assertPatched(SpringVersion.getVersion());
    }

    @Test
    void allSpringFrameworkModulesShareOneVersion() {
        String expected = SpringVersion.getVersion();
        assertNotNull(expected, "Spring Framework version is not available from the jar manifest");
        for (Class<?> type : List.of(BeanFactory.class, ApplicationContext.class, ExpressionParser.class,
                AopProxy.class, RestTemplate.class, DispatcherServlet.class)) {
            assertEquals(expected, type.getPackage().getImplementationVersion(),
                    type.getName() + " comes from a different Spring Framework version than spring-core");
        }
    }

    private static void assertPatched(String version) {
        assertNotNull(version, "Spring Framework version is not available from the jar manifest");
        int[] actual = Arrays.stream(version.split("[.-]", 4)).limit(3).mapToInt(Integer::parseInt).toArray();
        assertTrue(Arrays.compare(actual, MINIMUM) >= 0,
                "Spring Framework " + version + " is affected by CVE-2026-41852; expected >= 7.0.8");
    }
}
