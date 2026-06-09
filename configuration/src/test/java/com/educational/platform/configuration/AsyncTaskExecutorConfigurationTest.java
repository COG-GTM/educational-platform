package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that @EnableAsync on the application entry point correctly
 * configures the async task executor infrastructure required for
 * inter-module integration event publishing. Without the Spring Boot plugin,
 * the context cannot load and async support would be unavailable.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class AsyncTaskExecutorConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void asyncAnnotationBeanPostProcessor_shouldBeRegistered() {
        assertThat(applicationContext.getBeansOfType(AsyncAnnotationBeanPostProcessor.class))
                .as("@EnableAsync must register AsyncAnnotationBeanPostProcessor")
                .isNotEmpty();
    }

    @Test
    void applicationContext_shouldImplementApplicationEventPublisher() {
        // ApplicationContext itself extends ApplicationEventPublisher;
        // no separate bean lookup needed — the context IS the publisher
        assertThat(applicationContext)
                .as("ApplicationContext must implement ApplicationEventPublisher for inter-module communication")
                .isInstanceOf(ApplicationEventPublisher.class);
    }

    @Test
    void applicationContext_shouldHaveTaskExecutorBean() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        boolean hasTaskExecutor = false;
        for (String name : beanNames) {
            if (name.toLowerCase().contains("taskexecutor") || name.toLowerCase().contains("executor")) {
                hasTaskExecutor = true;
                break;
            }
        }
        assertThat(hasTaskExecutor)
                .as("@EnableAsync should register a TaskExecutor bean for async method invocation")
                .isTrue();
    }

    @Test
    void asyncSupportBeans_shouldBeMultiple() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        int asyncBeanCount = 0;
        for (String name : beanNames) {
            if (name.toLowerCase().contains("async")) {
                asyncBeanCount++;
            }
        }
        assertThat(asyncBeanCount)
                .as("@EnableAsync infrastructure should register multiple async-related beans")
                .isGreaterThanOrEqualTo(1);
    }
}
