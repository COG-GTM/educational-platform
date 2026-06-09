package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Functional test proving that the @MockitoBean annotation (provided by
 * spring-boot-starter-test) works within the application context.
 * SpringBootStarterTestValidationTest only checks classpath availability;
 * this test exercises the actual bean-override mechanism, confirming that
 * the starter-test dependency integrates correctly with the Spring Boot plugin.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class MockitoBeanIntegrationTest {

    @MockitoBean
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void mockitoBean_shouldReplaceRealBeanInContext() {
        assertThat(eventPublisher)
                .as("@MockitoBean must inject a Mockito mock into the application context")
                .isNotNull();
    }

    @Test
    void mockitoBean_shouldBeVerifiable() {
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void mockitoBean_shouldSupportStubbing() {
        // given — eventPublisher is a mock; publishing an event should be a no-op
        Object testEvent = new Object();

        // when
        eventPublisher.publishEvent(testEvent);

        // then
        verify(eventPublisher).publishEvent(testEvent);
    }

    @Test
    void applicationContext_shouldStillContainNonMockedBeans() {
        assertThat(applicationContext.getBean(DataSource.class))
                .as("Non-mocked beans must still be available in the context")
                .isNotNull();
    }
}
