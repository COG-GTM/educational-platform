package com.educational.platform.users.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how {@code com.educational.platform.security.api-docs.public} is bound into {@link WebSecurityConfig}.
 * <p>
 * The flag opens the API docs anonymously, so a misconfigured value must fail the application start
 * (fail closed) rather than being silently coerced to either {@code true} or {@code false}.
 */
class WebSecurityConfigFlagBindingTest {

    private static final String SECURITY_ENABLED = "com.educational.platform.security.enabled=true";
    private static final String API_DOCS_PUBLIC = "com.educational.platform.security.api-docs.public=";

    private final WebApplicationContextRunner sut = new WebApplicationContextRunner()
            .withUserConfiguration(WebSecurityConfig.class, WebSecurityConfigTest.Fixture.class);

    @Test
    void flagAbsent_contextStartsWithSecurityFilterChain() {
        sut.withPropertyValues(SECURITY_ENABLED)
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(SecurityFilterChain.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false", "TRUE", "False"})
    void flagWellFormed_contextStarts(String value) {
        sut.withPropertyValues(SECURITY_ENABLED, API_DOCS_PUBLIC + value)
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(SecurityFilterChain.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "maybe", "ture", "public", "null"})
    void flagMalformed_contextFailsToStart(String value) {
        sut.withPropertyValues(SECURITY_ENABLED, API_DOCS_PUBLIC + value)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void securityDisabled_webSecurityConfigNotRegistered() {
        sut.withPropertyValues(API_DOCS_PUBLIC + "true")
                .run(context -> assertThat(context).hasNotFailed()
                        .doesNotHaveBean(WebSecurityConfig.class)
                        .doesNotHaveBean(SecurityFilterChain.class));
    }
}
