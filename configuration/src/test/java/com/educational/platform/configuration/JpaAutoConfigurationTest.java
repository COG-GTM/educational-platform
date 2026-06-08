package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Spring Boot plugin activates JPA auto-configuration,
 * setting up the EntityManagerFactory and transaction management infrastructure.
 * All bounded context modules depend on JPA for persistence; without the
 * Spring Boot plugin, JPA auto-configuration would not activate.
 * Uses ApplicationContext bean lookups to avoid direct JPA imports which are
 * not on the configuration module's own test classpath.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class JpaAutoConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void entityManagerFactory_shouldBeAutoConfigured() {
        assertThat(applicationContext.containsBean("entityManagerFactory"))
                .as("EntityManagerFactory must be auto-configured for JPA persistence")
                .isTrue();
    }

    @Test
    void entityManagerFactory_shouldNotBeNull() {
        Object emf = applicationContext.getBean("entityManagerFactory");
        assertThat(emf)
                .as("EntityManagerFactory bean must be instantiated by Spring Boot auto-configuration")
                .isNotNull();
    }

    @Test
    void transactionManager_shouldBeRegistered() {
        assertThat(applicationContext.containsBean("transactionManager"))
                .as("PlatformTransactionManager must be registered for @Transactional support")
                .isTrue();
    }

    @Test
    void jpaDialect_shouldBeH2() {
        String dialect = applicationContext.getEnvironment()
                .getProperty("spring.jpa.database-platform");
        assertThat(dialect)
                .as("JPA dialect must be H2 as declared in application.properties")
                .isNotNull()
                .contains("H2");
    }

    @Test
    void jpaVendorAdapter_shouldBeAutoConfigured() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        boolean hasJpaBean = false;
        for (String name : beanNames) {
            if (name.toLowerCase().contains("jpa") || name.toLowerCase().contains("entitymanager")) {
                hasJpaBean = true;
                break;
            }
        }
        assertThat(hasJpaBean)
                .as("JPA-related beans must be registered by Spring Boot auto-configuration")
                .isTrue();
    }
}
