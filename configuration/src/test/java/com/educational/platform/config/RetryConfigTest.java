package com.educational.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

class RetryConfigTest {

    @Test
    void retryConfig_hasEnableRetryAnnotation() {
        // then
        assertThat(RetryConfig.class.getAnnotation(EnableRetry.class)).isNotNull();
    }

    @Test
    void retryConfig_hasConfigurationAnnotation() {
        // then
        assertThat(RetryConfig.class.getAnnotation(Configuration.class)).isNotNull();
    }

    @Test
    void retryConfig_canBeInstantiated() {
        // when
        RetryConfig config = new RetryConfig();

        // then
        assertThat(config).isNotNull();
    }

    @Test
    void enableRetry_hasLowestPrecedenceOrder() {
        EnableRetry enableRetry = RetryConfig.class.getAnnotation(EnableRetry.class);
        assertThat(enableRetry).isNotNull();
        assertThat(enableRetry.order()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    void enableRetry_proxyTargetClassDefaultIsFalse() {
        EnableRetry enableRetry = RetryConfig.class.getAnnotation(EnableRetry.class);
        assertThat(enableRetry).isNotNull();
        assertThat(enableRetry.proxyTargetClass()).isFalse();
    }

    @Test
    void retryConfig_orderIsLowerThanAsyncOrder() {
        EnableRetry enableRetry = RetryConfig.class.getAnnotation(EnableRetry.class);
        EnableAsync enableAsync = AsyncConfig.class.getAnnotation(EnableAsync.class);
        assertThat(enableRetry.order()).isGreaterThan(enableAsync.order());
    }

    @Test
    void retryConfig_doesNotHaveEnableAsync() {
        assertThat(RetryConfig.class.getAnnotation(EnableAsync.class)).isNull();
    }

    @Test
    void retryConfig_hasNoDeclaredMethods() {
        assertThat(RetryConfig.class.getDeclaredMethods()).as("Pure config class should have no methods").isEmpty();
    }

    @Test
    void retryConfig_doesNotExtendCustomBaseClass() {
        assertThat(RetryConfig.class.getSuperclass()).isEqualTo(Object.class);
    }

    @Test
    void retryConfig_hasNoDeclaredFields() {
        assertThat(RetryConfig.class.getDeclaredFields()).as("Pure config class should have no fields").isEmpty();
    }

    @Test
    void retryConfig_isNotAbstract() {
        assertThat(java.lang.reflect.Modifier.isAbstract(RetryConfig.class.getModifiers())).isFalse();
    }

    @Test
    void retryConfig_isNotFinal() {
        assertThat(java.lang.reflect.Modifier.isFinal(RetryConfig.class.getModifiers())).isFalse();
    }

    @Test
    void retryConfig_isPublic() {
        assertThat(java.lang.reflect.Modifier.isPublic(RetryConfig.class.getModifiers())).isTrue();
    }

    @Test
    void retryConfig_doesNotImplementAnyInterface() {
        assertThat(RetryConfig.class.getInterfaces()).isEmpty();
    }

    @Test
    void retryConfig_hasNoDeclaredConstructors_besideDefault() {
        assertThat(RetryConfig.class.getDeclaredConstructors()).hasSize(1);
        assertThat(RetryConfig.class.getDeclaredConstructors()[0].getParameterCount()).isZero();
    }

    @Test
    void enableRetry_orderValue_isIntegerMaxValue() {
        EnableRetry enableRetry = RetryConfig.class.getAnnotation(EnableRetry.class);
        assertThat(enableRetry).isNotNull();
        assertThat(enableRetry.order())
                .as("@EnableRetry.order should be Integer.MAX_VALUE (LOWEST_PRECEDENCE) — "
                        + "retry advisor wraps closest to target method, inside async proxy")
                .isEqualTo(Integer.MAX_VALUE);
    }
}
