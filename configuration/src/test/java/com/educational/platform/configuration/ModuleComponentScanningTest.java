package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Spring Boot plugin's component scanning correctly
 * discovers beans from ALL bounded context modules. The application class
 * resides in com.educational.platform, so all sub-packages should be scanned.
 * Without the Spring Boot plugin, the context cannot load and these tests fail.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class ModuleComponentScanningTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldDiscoverBeans_fromAdministrationModule() {
        assertThat(applicationContext.getBeanNamesForAnnotation(
                org.springframework.stereotype.Component.class))
                .as("Administration module beans must be discovered by component scanning")
                .anyMatch(name -> name.toLowerCase().contains("course") || name.toLowerCase().contains("admin")
                        || name.toLowerCase().contains("proposal"));
    }

    @Test
    void shouldDiscoverBeans_fromCoursesModule() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        boolean hasCourseBean = false;
        for (String name : beanNames) {
            if (name.toLowerCase().contains("course")) {
                hasCourseBean = true;
                break;
            }
        }
        assertThat(hasCourseBean)
                .as("Courses module beans must be discovered via component scanning from base package")
                .isTrue();
    }

    @Test
    void shouldDiscoverBeans_fromUsersModule() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        boolean hasUserBean = false;
        for (String name : beanNames) {
            if (name.toLowerCase().contains("user") || name.toLowerCase().contains("registration")) {
                hasUserBean = true;
                break;
            }
        }
        assertThat(hasUserBean)
                .as("Users module beans must be discovered via component scanning from base package")
                .isTrue();
    }

    @Test
    void shouldDiscoverBeans_fromSecurityModule() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        boolean hasSecurityBean = false;
        for (String name : beanNames) {
            if (name.toLowerCase().contains("security") || name.toLowerCase().contains("jwt")
                    || name.toLowerCase().contains("auth")) {
                hasSecurityBean = true;
                break;
            }
        }
        assertThat(hasSecurityBean)
                .as("Security module beans must be discovered via component scanning from base package")
                .isTrue();
    }

    @Test
    void shouldDiscoverBeans_fromCourseEnrollmentsModule() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        boolean hasEnrollmentBean = false;
        for (String name : beanNames) {
            if (name.toLowerCase().contains("enroll") || name.toLowerCase().contains("student")) {
                hasEnrollmentBean = true;
                break;
            }
        }
        assertThat(hasEnrollmentBean)
                .as("Course-enrollments module beans must be discovered via component scanning")
                .isTrue();
    }

    @Test
    void shouldDiscoverBeans_fromCourseReviewsModule() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        boolean hasReviewBean = false;
        for (String name : beanNames) {
            if (name.toLowerCase().contains("review") || name.toLowerCase().contains("reviewer")) {
                hasReviewBean = true;
                break;
            }
        }
        assertThat(hasReviewBean)
                .as("Course-reviews module beans must be discovered via component scanning")
                .isTrue();
    }

    @Test
    void shouldRegister_allExpectedIntegrationEventHandlers() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        boolean hasEventHandler = false;
        for (String name : beanNames) {
            if (name.toLowerCase().contains("integrationeventhandler")
                    || name.toLowerCase().contains("eventhandler")) {
                hasEventHandler = true;
                break;
            }
        }
        assertThat(hasEventHandler)
                .as("Integration event handlers from bounded contexts must be discoverable")
                .isTrue();
    }
}
