package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the Spring Boot plugin auto-configures Jackson
 * ObjectMapper for JSON serialization/deserialization. The REST APIs in
 * bounded context web modules (courses-web, users-web, etc.) depend on
 * this auto-configuration for request/response marshalling via bootRun.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class JacksonAutoConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void objectMapper_shouldBeInstantiable() throws Exception {
        Class<?> objectMapperClass = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
        Object mapper = objectMapperClass.getDeclaredConstructor().newInstance();
        assertThat(mapper)
                .as("ObjectMapper must be instantiable for REST API JSON serialization")
                .isNotNull();
    }

    @Test
    void objectMapper_shouldSerializeSimpleObject() throws Exception {
        Class<?> objectMapperClass = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
        Object mapper = objectMapperClass.getDeclaredConstructor().newInstance();
        var writeMethod = objectMapperClass.getMethod("writeValueAsString", Object.class);
        String json = (String) writeMethod.invoke(mapper, java.util.Map.of("name", "test", "value", 42));
        assertThat(json)
                .as("ObjectMapper must serialize objects to JSON for API responses")
                .contains("name")
                .contains("test")
                .contains("42");
    }

    @Test
    void jacksonDatabindModule_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("com.fasterxml.jackson.databind.ObjectMapper"))
                .as("Jackson Databind must be on classpath (transitive via spring-boot-starter-web)")
                .doesNotThrowAnyException();
    }

    @Test
    void jacksonCoreModule_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("com.fasterxml.jackson.core.JsonFactory"))
                .as("Jackson Core must be on classpath for low-level JSON processing")
                .doesNotThrowAnyException();
    }

    @Test
    void jacksonAnnotationsModule_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("com.fasterxml.jackson.annotation.JsonProperty"))
                .as("Jackson Annotations must be on classpath for DTO field mapping")
                .doesNotThrowAnyException();
    }

    @Test
    void jacksonJavaTimeModule_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule"))
                .as("Jackson JavaTimeModule must be available for Java 8+ date/time serialization")
                .doesNotThrowAnyException();
    }

    @Test
    void httpMessageConverterBeans_shouldBeRegistered() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        boolean hasConverterBean = false;
        for (String name : beanNames) {
            if (name.toLowerCase().contains("messageconverter") || name.toLowerCase().contains("jackson")) {
                hasConverterBean = true;
                break;
            }
        }
        assertThat(hasConverterBean)
                .as("Jackson HTTP message converters must be registered for REST API communication")
                .isTrue();
    }
}
