package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the Spring Boot condition evaluation infrastructure works
 * correctly. The Spring Boot plugin activates auto-configuration, which uses
 * {@code @ConditionalOnClass}, {@code @ConditionalOnProperty}, and other
 * conditions to decide which beans to register. These tests verify the
 * condition mechanism itself functions as expected — a prerequisite for
 * all auto-configured beans (DataSource, JPA, Jackson, etc.) to activate
 * correctly during bootRun.
 */
class SpringBootConditionEvaluationReportTest {

    @Test
    void conditionEvaluationReport_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport"))
                .as("ConditionEvaluationReport must be available for --debug auto-config diagnostics")
                .doesNotThrowAnyException();
    }

    @Test
    void conditionalOnProperty_shouldRespectPropertyValue() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.main.banner-mode=off")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getEnvironment().getProperty("spring.main.banner-mode"))
                            .as("Property-based condition must resolve the configured value")
                            .isEqualTo("off");
                });
    }

    @Test
    void conditionalBean_shouldBeRegistered_whenConditionIsTrue() {
        new ApplicationContextRunner()
                .withPropertyValues("test.condition.enabled=true")
                .withUserConfiguration(ConditionalBeanConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.containsBean("conditionalTestBean"))
                            .as("Bean must be registered when condition evaluates to true")
                            .isTrue();
                });
    }

    @Test
    void conditionalBean_shouldNotBeRegistered_whenConditionIsFalse() {
        new ApplicationContextRunner()
                .withPropertyValues("test.condition.enabled=false")
                .withUserConfiguration(ConditionalBeanConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.containsBean("conditionalTestBean"))
                            .as("Bean must not be registered when condition evaluates to false")
                            .isFalse();
                });
    }

    @Test
    void autoConfigurationImportSelector_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.autoconfigure.AutoConfigurationImportSelector"))
                .as("AutoConfigurationImportSelector must be available — it drives all @EnableAutoConfiguration processing")
                .doesNotThrowAnyException();
    }

    @Test
    void conditionOutcome_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.autoconfigure.condition.ConditionOutcome"))
                .as("ConditionOutcome must be available for condition evaluation results")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootCondition_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.autoconfigure.condition.SpringBootCondition"))
                .as("SpringBootCondition base class must be on classpath for custom condition implementations")
                .doesNotThrowAnyException();
    }

    @Configuration
    static class ConditionalBeanConfig {

        @Bean
        @Conditional(PropertyEnabledCondition.class)
        String conditionalTestBean() {
            return "enabled";
        }
    }

    static class PropertyEnabledCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return "true".equals(context.getEnvironment().getProperty("test.condition.enabled"));
        }
    }
}
