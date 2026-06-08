package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Functional test proving that the @MockitoSpyBean annotation (provided by
 * spring-boot-starter-test) works within the application context.
 * Complements {@link MockitoBeanIntegrationTest} which validates @MockitoBean;
 * this test validates the spy variant that wraps a real bean with Mockito
 * spy behavior, allowing verification of real method invocations.
 * In Spring Boot 4.x, @MockitoSpyBean replaces the legacy @SpyBean annotation.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class MockitoSpyBeanIntegrationTest {

    @MockitoSpyBean
    private DataSource dataSource;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void mockitoSpyBean_shouldWrapRealBeanInContext() {
        assertThat(dataSource)
                .as("@MockitoSpyBean must inject a Mockito spy wrapping the real DataSource bean")
                .isNotNull();
    }

    @Test
    void mockitoSpyBean_shouldRetainRealBehavior() throws Exception {
        // Spy wraps the real bean, so getConnection should use the real H2 DataSource
        // We just verify it doesn't throw — proving the real bean is still functional
        try (var connection = dataSource.getConnection()) {
            assertThat(connection)
                    .as("Spy-wrapped DataSource must still return a real connection from H2")
                    .isNotNull();
        }
    }

    @Test
    void mockitoSpyBean_shouldSupportVerification() throws Exception {
        // Exercise the spy
        try (var ignored = dataSource.getConnection()) {
            // no-op — just trigger the method
        }

        // Verify the spy recorded the invocation
        verify(dataSource, atLeastOnce()).getConnection();
    }

    @Test
    void applicationContext_shouldStillContainOtherBeans() {
        assertThat(applicationContext.containsBean("educationalPlatformApplication"))
                .as("Application bean must still be present alongside the spy")
                .isTrue();
    }
}
